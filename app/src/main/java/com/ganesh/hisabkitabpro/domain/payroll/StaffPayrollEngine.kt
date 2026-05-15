package com.ganesh.hisabkitabpro.domain.payroll

import com.ganesh.hisabkitabpro.data.local.StaffAttendanceEntity
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.data.local.StaffPayrollEntryEntity

/**
 * Pure-Kotlin payroll calculation engine.
 *
 * Inputs are deterministic data classes; no Android, no Room, no I/O. This
 * lets the entire payroll math run in a unit test on the JVM and guarantees
 * that pay computation can NEVER hit the network or the customer ledger.
 *
 * Money is in **paise** (Long) — never `Double` — to avoid float-rounding
 * drift on long monthly cycles.
 */
object StaffPayrollEngine {

    /**
     * Snapshot of payroll for one staff member over one bounded period.
     */
    data class PayrollResult(
        val staffId: String,
        val periodStartMillis: Long,
        val periodEndMillisExclusive: Long,
        val baseSalaryPaise: Long,
        val effectiveDays: Double,
        val totalDays: Int,
        val presentCount: Int,
        val absentCount: Int,
        val halfDayCount: Int,
        val leaveCount: Int,
        val earnedPaise: Long,
        val advancesPaise: Long,
        val bonusesPaise: Long,
        val deductionsPaise: Long,
        val salaryPaidPaise: Long,
        /**
         * Loss of Pay = portion of base salary forfeited due to non-attendance.
         *
         * For MONTHLY salary types this is `max(baseSalary - earned, 0)`. For
         * DAILY / WEEKLY types there is no implicit "base" to forfeit so this is
         * always `0` — those staff are paid strictly per attended day.
         */
        val lossOfPayPaise: Long,
        val netPayablePaise: Long
    )

    /**
     * Compute payroll for a single staff member over `[periodStart, periodEnd)`.
     *
     * - `MONTHLY` uses calendar-month proration based on attendance days.
     * - `DAILY`   pays per attended day (half-day → 0.5).
     * - `WEEKLY`  divides the weekly base salary across the configured workdays
     *             and pays per attended day.
     */
    fun computeForPeriod(
        staff: StaffEntity,
        attendance: List<StaffAttendanceEntity>,
        payrollEntries: List<StaffPayrollEntryEntity>,
        periodStartMillis: Long,
        periodEndMillisExclusive: Long,
        totalCalendarDays: Int
    ): PayrollResult {
        require(periodEndMillisExclusive > periodStartMillis) {
            "Period end must be strictly after start"
        }
        require(totalCalendarDays > 0) { "totalCalendarDays must be > 0" }

        val present = attendance.count { it.status == StaffAttendanceEntity.STATUS_PRESENT }
        val absent = attendance.count { it.status == StaffAttendanceEntity.STATUS_ABSENT }
        val halfDay = attendance.count { it.status == StaffAttendanceEntity.STATUS_HALF_DAY }
        val leave = attendance.count { it.status == StaffAttendanceEntity.STATUS_LEAVE }

        val effectiveDays: Double = present.toDouble() + halfDay * 0.5

        val earnedPaise: Long = when (staff.salaryType) {
            StaffEntity.SALARY_TYPE_DAILY -> {
                roundToPaise(staff.salaryAmountPaise.toDouble() * effectiveDays)
            }
            StaffEntity.SALARY_TYPE_WEEKLY -> {
                val workdays = staff.workdaysPerWeek.coerceIn(1, 7)
                val perDay = staff.salaryAmountPaise.toDouble() / workdays
                roundToPaise(perDay * effectiveDays)
            }
            else -> {
                // MONTHLY (default). Pro-rate vs. calendar-month length.
                val perDay = staff.salaryAmountPaise.toDouble() / totalCalendarDays
                roundToPaise(perDay * effectiveDays)
            }
        }

        val advances = sumKind(payrollEntries, StaffPayrollEntryEntity.KIND_ADVANCE)
        val bonuses = sumKind(payrollEntries, StaffPayrollEntryEntity.KIND_BONUS)
        val deductions = sumKind(payrollEntries, StaffPayrollEntryEntity.KIND_DEDUCTION)
        val salaryPaid = sumKind(payrollEntries, StaffPayrollEntryEntity.KIND_SALARY_PAID)

        // Loss of Pay only applies to salary types with a fixed period base
        // (MONTHLY). For DAILY / WEEKLY pay there is no "base" to forfeit, so
        // we keep LoP = 0 and let `earned` represent actual pay alone.
        val lossOfPay: Long = when (staff.salaryType) {
            StaffEntity.SALARY_TYPE_MONTHLY ->
                (staff.salaryAmountPaise - earnedPaise).coerceAtLeast(0L)
            else -> 0L
        }

        // Net payable = earned + bonuses − advances − deductions − already-paid.
        // (LoP is implicit — already deducted from `earned` via attendance.)
        // Negative net means the staff has been over-paid / over-advanced — kept signed.
        val netPayable = earnedPaise + bonuses - advances - deductions - salaryPaid

        return PayrollResult(
            staffId = staff.id,
            periodStartMillis = periodStartMillis,
            periodEndMillisExclusive = periodEndMillisExclusive,
            baseSalaryPaise = staff.salaryAmountPaise,
            effectiveDays = effectiveDays,
            totalDays = totalCalendarDays,
            presentCount = present,
            absentCount = absent,
            halfDayCount = halfDay,
            leaveCount = leave,
            earnedPaise = earnedPaise,
            advancesPaise = advances,
            bonusesPaise = bonuses,
            deductionsPaise = deductions,
            salaryPaidPaise = salaryPaid,
            lossOfPayPaise = lossOfPay,
            netPayablePaise = netPayable
        )
    }

    /**
     * All-time outstanding balance — useful for the staff list summary.
     * Same accounting identity as [computeForPeriod] but unbounded.
     */
    fun lifetimeNetPayablePaise(
        attendanceEarnedPaise: Long,
        bonusesPaise: Long,
        advancesPaise: Long,
        deductionsPaise: Long,
        salaryPaidPaise: Long
    ): Long = attendanceEarnedPaise + bonusesPaise - advancesPaise - deductionsPaise - salaryPaidPaise

    private fun sumKind(entries: List<StaffPayrollEntryEntity>, kind: String): Long =
        entries.asSequence()
            .filter { it.kind == kind && it.isDeleted == 0 }
            .sumOf { it.amountPaise }

    private fun roundToPaise(value: Double): Long =
        if (value.isFinite()) Math.round(value) else 0L
}
