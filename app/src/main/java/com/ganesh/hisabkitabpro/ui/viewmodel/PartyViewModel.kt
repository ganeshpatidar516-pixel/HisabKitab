package com.ganesh.hisabkitabpro.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.addon.audit.AuditLogRecorder
import com.ganesh.hisabkitabpro.addon.reminder.SupplierPartyReminderScheduler
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.domain.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class PartyViewModel @Inject constructor(
    val repository: PartyRepository,
    private val auditLogRecorder: AuditLogRecorder,
    private val appDatabase: AppDatabase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    data class SupplierScanPrefill(
        val supplierId: Long,
        val amountText: String,
        val note: String,
        val billImageUri: String? = null,
        val lowConfidenceAmount: Boolean = false,
    )

    private val _isSupplierTab = MutableStateFlow(false)
    val isSupplierTab: StateFlow<Boolean> = _isSupplierTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _pendingSupplierScanPrefill = MutableStateFlow<SupplierScanPrefill?>(null)
    val pendingSupplierScanPrefill: StateFlow<SupplierScanPrefill?> = _pendingSupplierScanPrefill.asStateFlow()

    private val _supplierLedgerLogs = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val supplierLedgerLogs: StateFlow<List<AuditLogEntry>> = _supplierLedgerLogs.asStateFlow()

    fun refreshSupplierLedger(supplierId: Long) {
        viewModelScope.launch {
            val rows = withContext(Dispatchers.IO) {
                appDatabase.auditLogDao().recentByEntityId("SUPPLIER_LEDGER", supplierId, 300)
            }
            _supplierLedgerLogs.value = rows
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedParties: Flow<PagingData<Party>> = combine(
        _isSupplierTab,
        _searchQuery
    ) { isSupplier, query ->
        Pair(isSupplier, query)
    }.flatMapLatest { (isSupplier, query) ->
        repository.getPartiesPaged(isSupplier, query.ifBlank { null })
    }.cachedIn(viewModelScope)

    /** P0 — full-database supplier payable; supplier list header must not sum only loaded pages. */
    val activeSuppliersTotalBalancePaise: StateFlow<Long> =
        repository.observeActiveSuppliersTotalBalancePaise()
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSupplierTab(isSupplier: Boolean) {
        _isSupplierTab.value = isSupplier
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addParty(name: String, phone: String, isSupplier: Boolean, city: String? = null) {
        viewModelScope.launch {
            repository.addParty(Party(name = name, phone = phone, isSupplier = isSupplier, city = city))
        }
    }

    fun addParty(name: String, phone: String, isSupplier: Boolean, city: String? = null, onAdded: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addParty(Party(name = name, phone = phone, isSupplier = isSupplier, city = city))
            onAdded(id)
        }
    }

    fun setPendingSupplierScanPrefill(
        supplierId: Long,
        amountText: String,
        note: String,
        billImageUri: String? = null,
        lowConfidenceAmount: Boolean = false,
    ) {
        _pendingSupplierScanPrefill.value = SupplierScanPrefill(
            supplierId,
            amountText,
            note,
            billImageUri,
            lowConfidenceAmount,
        )
    }

    fun consumePendingSupplierScanPrefill() {
        _pendingSupplierScanPrefill.value = null
    }

    /**
     * Supplier ledger quick-entry:
     * - PURCHASE increases payable (positive balance).
     * - PAYMENT decreases payable.
     */
    fun recordSupplierEntry(
        supplierId: Long,
        amountPaise: Long,
        isPurchase: Boolean,
        note: String? = null,
        tag: String? = null,
        dueAt: Long? = null,
        billImageUri: String? = null,
        onDone: (Boolean) -> Unit = {}
    ) {
        if (amountPaise <= 0L) {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val supplier = repository.getPartyById(supplierId)
            if (supplier == null || !supplier.isSupplier) {
                onDone(false)
                return@launch
            }
            val delta = if (isPurchase) amountPaise else -amountPaise
            val updated = supplier.copy(
                totalBalance = supplier.totalBalance + delta,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateParty(updated)
            withContext(Dispatchers.IO) {
                SupplierPartyReminderScheduler.syncAfterPartySupplierBalanceChange(context, appDatabase, supplierId)
            }
            val action = if (isPurchase) "SUPPLIER_PURCHASE_RECORDED" else "SUPPLIER_PAYMENT_RECORDED"
            auditLogRecorder.recordAsync(
                entityType = "SUPPLIER_LEDGER",
                entityId = supplierId,
                action = action,
                detail = buildString {
                    append("amountPaise=").append(amountPaise)
                    append(",balanceAfter=").append(updated.totalBalance)
                    append(",tag=").append(Uri.encode(tag?.trim().orEmpty()))
                    append(",note=").append(Uri.encode(note?.trim().orEmpty()))
                    append(",dueAt=").append(dueAt ?: 0L)
                    append(",billImageUri=").append(billImageUri.orEmpty())
                }
            )
            onDone(true)
        }
    }
}
