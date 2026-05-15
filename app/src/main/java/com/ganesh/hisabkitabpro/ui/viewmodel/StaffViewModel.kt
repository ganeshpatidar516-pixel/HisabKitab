package com.ganesh.hisabkitabpro.ui.viewmodel

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ganesh.hisabkitabpro.addon.audit.AuditLogRecorder
import com.ganesh.hisabkitabpro.data.local.StaffAttendanceEntity
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.data.local.StaffPayrollEntryEntity
import com.ganesh.hisabkitabpro.data.repository.local.StaffAttendanceDao
import com.ganesh.hisabkitabpro.data.repository.local.StaffDao
import com.ganesh.hisabkitabpro.data.repository.local.StaffPayrollDao
import com.ganesh.hisabkitabpro.domain.cloud.CloudBusinessIdentity
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.ganesh.hisabkitabpro.domain.payroll.AttendanceDayKey
import com.ganesh.hisabkitabpro.domain.payroll.StaffPayrollEngine
import com.ganesh.hisabkitabpro.domain.payroll.StaffSalarySlipPdfGenerator
import com.ganesh.hisabkitabpro.domain.payroll.StaffSecureFields
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Staff & payroll surface for the detail / edit / list screens.
 *
 * **Zero-break invariants** (per workspace rule):
 * - Existing staff records are never mutated by these flows; soft-delete and
 *   restore are pure flag flips on the parent row.
 * - Attendance and payroll history are always preserved — archive does NOT
 *   cascade.
 * - Customer ledger / bill flow code is not touched.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StaffViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val staffDao: StaffDao,
    private val attendanceDao: StaffAttendanceDao,
    private val payrollDao: StaffPayrollDao,
    private val auditLogRecorder: AuditLogRecorder,
    private val cloudBusinessIdentity: CloudBusinessIdentity,
    private val selectiveCloudMirror: SelectiveCloudMirror
) : ViewModel() {

    private val businessId: String = cloudBusinessIdentity.currentBusinessId()

    /** Stream of all active staff (decrypted view-model copy). */
    val staffList: StateFlow<List<StaffEntity>> = staffDao.getAllStaffForBusiness(businessId)
        .map { rows -> rows.map { it.toDecryptedView() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Archived staff — for the secure restore / audit viewer. */
    val archivedStaff: StateFlow<List<StaffEntity>> = staffDao.getArchivedStaffForBusiness(businessId)
        .map { rows -> rows.map { it.toDecryptedView() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedStaffId = MutableStateFlow<String?>(null)
    val selectedStaffId: StateFlow<String?> = _selectedStaffId.asStateFlow()

    /** Detail screen — observe a single staff record (any archive state). */
    val selectedStaff: StateFlow<StaffEntity?> = _selectedStaffId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(null)
            else staffDao.observeStaffByIdAnyForBusiness(id, businessId).map { it?.toDecryptedView() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Attendance for the currently-selected month for the selected staff. */
    private val _attendancePeriodStart = MutableStateFlow(
        AttendanceDayKey.startOfMonth(System.currentTimeMillis())
    )
    val attendancePeriodStart: StateFlow<Long> = _attendancePeriodStart.asStateFlow()

    val attendanceForPeriod: StateFlow<List<StaffAttendanceEntity>> =
        combine(_selectedStaffId, _attendancePeriodStart) { id, start -> id to start }
            .flatMapLatest { (id, start) ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else attendanceDao.observeRange(
                    staffId = id,
                    fromMillis = start,
                    toMillis = AttendanceDayKey.endOfMonthExclusive(start) - 1
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Payroll entries in the currently-active period (for live recompute). */
    private val payrollEntriesForPeriod: StateFlow<List<StaffPayrollEntryEntity>> =
        combine(_selectedStaffId, _attendancePeriodStart) { id, start -> id to start }
            .flatMapLatest { (id, start) ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else payrollDao.observeRange(
                    staffId = id,
                    fromMillis = start,
                    toMillis = AttendanceDayKey.endOfMonthExclusive(start) - 1
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Lifetime payroll entries for the selected staff (for the passbook). */
    val payrollEntries: StateFlow<List<StaffPayrollEntryEntity>> =
        _selectedStaffId
            .flatMapLatest { id ->
                if (id.isNullOrBlank()) flowOf(emptyList())
                else payrollDao.observeAllForStaff(id)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Reactive Net Payable. Recomputes automatically whenever attendance,
     * payroll entries, the selected staff, or the period boundary change —
     * no manual refresh needed.
     */
    val currentPayroll: StateFlow<StaffPayrollEngine.PayrollResult?> = combine(
        selectedStaff,
        attendanceForPeriod,
        payrollEntriesForPeriod,
        _attendancePeriodStart
    ) { staff, attendance, entries, start ->
        if (staff == null) return@combine null
        val end = AttendanceDayKey.endOfMonthExclusive(start)
        val totalDays = AttendanceDayKey.daysInMonth(start)
        runCatching {
            StaffPayrollEngine.computeForPeriod(
                staff = staff,
                attendance = attendance,
                payrollEntries = entries,
                periodStartMillis = start,
                periodEndMillisExclusive = end,
                totalCalendarDays = totalDays
            )
        }.getOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _lastSlipPath = MutableStateFlow<String?>(null)
    val lastSlipPath: StateFlow<String?> = _lastSlipPath.asStateFlow()

    /** One-shot events for the salary-slip share / open intent. */
    private val _slipShareEvents = Channel<File>(Channel.BUFFERED)
    val slipShareEvents = _slipShareEvents.receiveAsFlow()

    private val _staffEvents = Channel<String>(Channel.BUFFERED)
    val staffEvents = _staffEvents.receiveAsFlow()

    fun selectStaff(id: String?) {
        _selectedStaffId.value = id
        if (id != null) {
            _attendancePeriodStart.value = AttendanceDayKey.startOfMonth(System.currentTimeMillis())
        }
    }

    fun setAttendancePeriodStart(epochMillis: Long) {
        _attendancePeriodStart.value = AttendanceDayKey.startOfMonth(epochMillis)
    }

    // ---------- staff CRUD ----------

    fun insertStaff(staff: StaffEntity) {
        viewModelScope.launch {
            val scopedStaff = staff.copy(businessId = businessId, updatedAt = System.currentTimeMillis())
            val secured = StaffSecureFields.encryptForWrite(scopedStaff)
            staffDao.insertStaff(secured)
            selectiveCloudMirror.syncStaffAccessRights(scopedStaff)
            audit(scopedStaff.id, "STAFF_CREATE", buildString {
                append("name=${scopedStaff.name}")
                append(", role=${scopedStaff.role}")
                append(", salaryType=${scopedStaff.salaryType}")
                append(", salaryPaise=${scopedStaff.salaryAmountPaise}")
            })
            notify("Staff created successfully.")
        }
    }

    fun updateStaff(staff: StaffEntity) {
        viewModelScope.launch {
            val previous = staffDao.getStaffById(staff.id)
            val scopedStaff = staff.copy(businessId = businessId, updatedAt = System.currentTimeMillis())
            val updated = StaffSecureFields.encryptForWrite(
                scopedStaff
            )
            staffDao.updateStaff(updated)
            selectiveCloudMirror.syncStaffAccessRights(scopedStaff)
            audit(scopedStaff.id, "STAFF_UPDATE", diffSummary(previous, scopedStaff))
            notify("Staff updated successfully.")
        }
    }

    /**
     * Soft-archive — preserves financial history. Cleared via [restoreStaff].
     */
    fun archiveStaff(id: String) {
        viewModelScope.launch {
            staffDao.softDeleteStaff(id)
            staffDao.getStaffByIdAny(id)
                ?.let { selectiveCloudMirror.syncStaffAccessRights(it.copy(isDeleted = 1)) }
            audit(id, "STAFF_ARCHIVE", null)
            notify("Staff archived successfully.")
        }
    }

    /**
     * Absolute delete: removes local staff metadata, payroll rows, attendance
     * rows, and the lightweight cloud staff-access/status document.
     */
    fun deleteStaff(id: String) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    attendanceDao.deleteForStaff(id)
                    payrollDao.deleteForStaff(id)
                    staffDao.hardDeleteStaff(id)
                    selectiveCloudMirror.deleteStaffAccessRights(id)
                }
            }.onSuccess {
                audit(id, "STAFF_HARD_DELETE", "local+cloud access removed")
                notify("Staff deleted successfully.")
            }.onFailure { e ->
                audit(id, "STAFF_DELETE_FAILED", e.message?.take(200))
                notify("Staff delete failed: ${e.message ?: "cloud cleanup error"}")
            }
        }
    }

    fun restoreStaff(id: String) {
        viewModelScope.launch {
            staffDao.restoreStaff(id)
            staffDao.getStaffByIdAny(id)
                ?.let { selectiveCloudMirror.syncStaffAccessRights(it.copy(isDeleted = 0)) }
            audit(id, "STAFF_RESTORE", null)
            notify("Staff restored successfully.")
        }
    }

    // ---------- attendance ----------

    fun markAttendance(
        staffId: String,
        epochMillis: Long,
        status: String,
        note: String = ""
    ) {
        viewModelScope.launch {
            val dayKey = AttendanceDayKey.startOfDay(epochMillis)
            val existing = attendanceDao.findForDay(staffId, dayKey)
            val now = System.currentTimeMillis()
            attendanceDao.upsert(
                StaffAttendanceEntity(
                    id = existing?.id ?: 0L,
                    staffId = staffId,
                    dateMillis = dayKey,
                    status = status,
                    note = note,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
            audit(
                staffId,
                if (existing == null) "ATTENDANCE_MARK" else "ATTENDANCE_UPDATE",
                "day=${formatDay(dayKey)}, status=$status"
            )
        }
    }

    fun clearAttendance(staffId: String, epochMillis: Long) {
        viewModelScope.launch {
            val dayKey = AttendanceDayKey.startOfDay(epochMillis)
            attendanceDao.deleteForDay(staffId, dayKey)
            audit(staffId, "ATTENDANCE_CLEAR", "day=${formatDay(dayKey)}")
        }
    }

    // ---------- payroll entries ----------

    fun addPayrollEntry(
        staffId: String,
        kind: String,
        amountPaise: Long,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val rowId = payrollDao.insert(
                StaffPayrollEntryEntity(
                    staffId = staffId,
                    kind = kind,
                    amountPaise = amountPaise,
                    note = note,
                    dateMillis = dateMillis,
                    cycleKey = monthCycleKey(dateMillis)
                )
            )
            auditLogRecorder.recordAsync(
                entityType = "STAFF_PAYROLL",
                entityId = rowId,
                action = "PAYROLL_ENTRY_ADD",
                detail = "staffId=$staffId, kind=$kind, paise=$amountPaise"
            )
        }
    }

    fun softDeletePayrollEntry(id: Long) {
        viewModelScope.launch {
            payrollDao.softDelete(id)
            auditLogRecorder.recordAsync(
                entityType = "STAFF_PAYROLL",
                entityId = id,
                action = "PAYROLL_ENTRY_REMOVE",
                detail = null
            )
        }
    }

    // ---------- payroll computation / slip ----------

    /**
     * Backwards-compatible synchronous compute for the current visible month.
     * The reactive [currentPayroll] is the preferred surface for UIs.
     */
    suspend fun computePayrollForCurrentMonth(staff: StaffEntity): StaffPayrollEngine.PayrollResult {
        val periodStart = _attendancePeriodStart.value
        val periodEndExclusive = AttendanceDayKey.endOfMonthExclusive(periodStart)
        val totalDays = AttendanceDayKey.daysInMonth(periodStart)

        val attendance = attendanceDao.fetchRange(
            staff.id,
            periodStart,
            periodEndExclusive - 1
        )
        val payroll = payrollDao.fetchRange(
            staff.id,
            periodStart,
            periodEndExclusive - 1
        )

        return StaffPayrollEngine.computeForPeriod(
            staff = staff,
            attendance = attendance,
            payrollEntries = payroll,
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEndExclusive,
            totalCalendarDays = totalDays
        )
    }

    /**
     * Generate the salary-slip PDF on the IO dispatcher. The file path is
     * cached on [lastSlipPath] and the file is also pushed to
     * [slipShareEvents] so the UI can open the system share sheet.
     */
    suspend fun generateSalarySlipPdf(staff: StaffEntity): File = withContext(Dispatchers.IO) {
        val result = computePayrollForCurrentMonth(staff)
        val periodLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(Date(result.periodStartMillis))
        val file = StaffSalarySlipPdfGenerator(appContext).generate(
            staff = staff,
            result = result,
            periodLabel = periodLabel
        )
        _lastSlipPath.value = file.absolutePath
        // Best-effort emit — never block PDF generation on UI consumer state.
        runCatching { _slipShareEvents.trySend(file) }
        auditLogRecorder.recordAsync(
            entityType = "STAFF",
            entityId = staff.id.hashCode().toLong(),
            action = "SLIP_GENERATED",
            detail = "period=$periodLabel, net=${result.netPayablePaise}"
        )
        file
    }

    private fun monthCycleKey(epochMillis: Long): String {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMillis }
        val year = cal.get(java.util.Calendar.YEAR)
        val month = cal.get(java.util.Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    private fun StaffEntity.toDecryptedView(): StaffEntity {
        // Surface the decrypted phone in `phone` so existing UI keeps working.
        // We never persist this transformation back to the DB — only the view-
        // model copy is decrypted; the encrypted column remains the source.
        val phonePlain = StaffSecureFields.decryptedPhone(this)
        return if (phonePlain == this.phone) this else copy(phone = phonePlain)
    }

    // ---------- audit helpers ----------

    private fun audit(staffId: String, action: String, detail: String?) {
        auditLogRecorder.recordAsync(
            entityType = "STAFF",
            entityId = staffId.hashCode().toLong(),
            action = action,
            detail = detail?.let { "id=$staffId; $it" } ?: "id=$staffId"
        )
    }

    private fun diffSummary(before: StaffEntity?, after: StaffEntity): String {
        if (before == null) return "first-edit, name=${after.name}"
        val changes = mutableListOf<String>()
        if (before.name != after.name) changes += "name:${before.name}→${after.name}"
        if (before.role != after.role) changes += "role:${before.role}→${after.role}"
        if (before.designation != after.designation)
            changes += "designation:${before.designation}→${after.designation}"
        if (before.salaryType != after.salaryType)
            changes += "salaryType:${before.salaryType}→${after.salaryType}"
        if (before.salaryAmountPaise != after.salaryAmountPaise)
            changes += "salaryPaise:${before.salaryAmountPaise}→${after.salaryAmountPaise}"
        if (before.workdaysPerWeek != after.workdaysPerWeek)
            changes += "workdays:${before.workdaysPerWeek}→${after.workdaysPerWeek}"
        if (before.isActive != after.isActive)
            changes += "isActive:${before.isActive}→${after.isActive}"
        if (before.address != after.address) changes += "address-changed"
        if (before.notes != after.notes) changes += "notes-changed"
        return if (changes.isEmpty()) "no-op" else changes.joinToString(", ")
    }

    private fun formatDay(epochMillis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(epochMillis))

    private fun notify(message: String) {
        _staffEvents.trySend(message)
        Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
    }
}
