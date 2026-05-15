package com.ganesh.hisabkitabpro.domain.payroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AttendanceDayKeyTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun startOfDayIsIdempotent() {
        val noon = 1_704_106_800_000L // 1 Jan 2024 11:00 UTC
        val a = AttendanceDayKey.startOfDay(noon, utc)
        val b = AttendanceDayKey.startOfDay(a, utc)
        assertEquals(a, b)
    }

    @Test
    fun startOfMonthIsFirstOfMonthMidnight() {
        val mid = 1_705_276_800_000L // 15 Jan 2024 00:00 UTC
        val start = AttendanceDayKey.startOfMonth(mid, utc)
        val cal = Calendar.getInstance(utc).apply { timeInMillis = start }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun endOfMonthExclusiveLandsOnFirstOfNextMonth() {
        val jan = 1_705_276_800_000L
        val end = AttendanceDayKey.endOfMonthExclusive(jan, utc)
        val cal = Calendar.getInstance(utc).apply { timeInMillis = end }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
    }

    @Test
    fun daysInJanuaryIs31AndFebruaryIsAtLeast28() {
        val jan = 1_705_276_800_000L
        val feb = 1_707_955_200_000L // 15 Feb 2024 UTC (leap year → 29)
        assertEquals(31, AttendanceDayKey.daysInMonth(jan, utc))
        assertTrue(AttendanceDayKey.daysInMonth(feb, utc) in 28..29)
    }
}
