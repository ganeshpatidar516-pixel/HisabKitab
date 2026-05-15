package com.ganesh.hisabkitabpro.domain.payroll

import com.ganesh.hisabkitabpro.data.local.StaffAttendanceEntity
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import com.ganesh.hisabkitabpro.data.local.StaffPayrollEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffPayrollEngineTest {

    private val periodStart = 1_704_067_200_000L // 1 Jan 2024 UTC
    private val periodEnd = 1_706_745_600_000L  // 1 Feb 2024 UTC
    private val staffMonthly = StaffEntity(
        id = "s1",
        name = "Asha",
        phone = "9999999999",
        businessId = "default_business",
        salaryType = StaffEntity.SALARY_TYPE_MONTHLY,
        salaryAmountPaise = 31_000_00L
    )

    private fun att(day: Long, status: String) = StaffAttendanceEntity(
        staffId = "s1",
        dateMillis = day,
        status = status
    )

    private fun entry(kind: String, paise: Long) = StaffPayrollEntryEntity(
        staffId = "s1",
        kind = kind,
        amountPaise = paise,
        dateMillis = periodStart
    )

    @Test
    fun monthlyProrationFullAttendancePaysFullSalary() {
        // 31 present days in a 31-day month → full salary, zero loss-of-pay
        val attendance = (0L until 31L).map {
            att(periodStart + it * 86_400_000L, StaffAttendanceEntity.STATUS_PRESENT)
        }
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = attendance,
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        assertEquals(31_000_00L, result.earnedPaise)
        assertEquals(0L, result.lossOfPayPaise)
        assertEquals(31_000_00L, result.netPayablePaise)
    }

    @Test
    fun monthlyAbsenteeismRaisesLossOfPay() {
        // Half the month present → earned = 50% of base, LoP = the other 50%
        val attendance = (0L until 16L).map {
            att(periodStart + it * 86_400_000L, StaffAttendanceEntity.STATUS_PRESENT)
        }
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = attendance,
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // 31_00_000 / 31 = 100_000 paise/day × 16 days = 16_00_000
        assertEquals(16_00_000L, result.earnedPaise)
        // LoP = base − earned = 31_00_000 − 16_00_000 = 15_00_000
        assertEquals(15_00_000L, result.lossOfPayPaise)
    }

    @Test
    fun dailyTypeNeverHasLossOfPay() {
        val staff = staffMonthly.copy(
            salaryType = StaffEntity.SALARY_TYPE_DAILY,
            salaryAmountPaise = 800_00L
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staff,
            attendance = emptyList(),
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // No fixed monthly base → LoP must always be zero
        assertEquals(0L, result.lossOfPayPaise)
    }

    @Test
    fun halfDayCountsAsZeroPointFive() {
        val attendance = listOf(
            att(periodStart, StaffAttendanceEntity.STATUS_HALF_DAY)
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = attendance,
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // 31000 / 31 = 1000 per day; half = 500.00 INR = 50_000 paise
        assertEquals(50_000L, result.earnedPaise)
        assertEquals(0.5, result.effectiveDays, 0.0001)
    }

    @Test
    fun advancesAndDeductionsReduceNetPayable() {
        val attendance = listOf(
            att(periodStart, StaffAttendanceEntity.STATUS_PRESENT)
        )
        val payroll = listOf(
            entry(StaffPayrollEntryEntity.KIND_ADVANCE, 30_000L),
            entry(StaffPayrollEntryEntity.KIND_BONUS, 50_000L),
            entry(StaffPayrollEntryEntity.KIND_DEDUCTION, 10_000L),
            entry(StaffPayrollEntryEntity.KIND_SALARY_PAID, 5_000L)
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = attendance,
            payrollEntries = payroll,
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // earned = 1 day → 100_000 paise (1000.00 INR)
        // net = 100_000 + 50_000 − 30_000 − 10_000 − 5_000 = 105_000
        assertEquals(100_000L, result.earnedPaise)
        assertEquals(105_000L, result.netPayablePaise)
    }

    @Test
    fun dailyTypePaysExactlyPerAttendedDay() {
        val staff = staffMonthly.copy(
            salaryType = StaffEntity.SALARY_TYPE_DAILY,
            salaryAmountPaise = 800_00L // 800 INR / day
        )
        val attendance = listOf(
            att(periodStart, StaffAttendanceEntity.STATUS_PRESENT),
            att(periodStart + 86_400_000L, StaffAttendanceEntity.STATUS_HALF_DAY),
            att(periodStart + 2 * 86_400_000L, StaffAttendanceEntity.STATUS_ABSENT)
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staff,
            attendance = attendance,
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // 1 + 0.5 days = 1200.00 INR = 120_000 paise
        assertEquals(120_000L, result.earnedPaise)
    }

    @Test
    fun weeklyTypeSplitsPayAcrossConfiguredWorkdays() {
        val staff = staffMonthly.copy(
            salaryType = StaffEntity.SALARY_TYPE_WEEKLY,
            salaryAmountPaise = 6_000_00L,
            workdaysPerWeek = 6
        )
        val attendance = listOf(
            att(periodStart, StaffAttendanceEntity.STATUS_PRESENT),
            att(periodStart + 86_400_000L, StaffAttendanceEntity.STATUS_PRESENT)
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staff,
            attendance = attendance,
            payrollEntries = emptyList(),
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // 6000 / 6 = 1000 per day, 2 days → 2000 INR
        assertEquals(200_000L, result.earnedPaise)
    }

    @Test
    fun deletedPayrollEntriesAreIgnored() {
        val attendance = listOf(att(periodStart, StaffAttendanceEntity.STATUS_PRESENT))
        val payroll = listOf(
            entry(StaffPayrollEntryEntity.KIND_BONUS, 50_000L).copy(isDeleted = 1),
            entry(StaffPayrollEntryEntity.KIND_BONUS, 25_000L)
        )
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = attendance,
            payrollEntries = payroll,
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        // bonuses should reflect only the active entry
        assertEquals(25_000L, result.bonusesPaise)
    }

    @Test
    fun zeroAttendanceProducesZeroEarnedAndNegativeNetIfPaid() {
        val payroll = listOf(entry(StaffPayrollEntryEntity.KIND_SALARY_PAID, 50_000L))
        val result = StaffPayrollEngine.computeForPeriod(
            staff = staffMonthly,
            attendance = emptyList(),
            payrollEntries = payroll,
            periodStartMillis = periodStart,
            periodEndMillisExclusive = periodEnd,
            totalCalendarDays = 31
        )
        assertEquals(0L, result.earnedPaise)
        assertEquals(-50_000L, result.netPayablePaise)
        assertTrue(result.netPayablePaise < 0)
    }
}
