package com.ganesh.hisabkitabpro.domain.payroll

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/**
 * Canonical day-key normalizer for attendance.
 *
 * The same calendar day must hash to the same `dateMillis` regardless of when
 * it was logged inside the day. We snap to local-midnight in the supplied
 * [TimeZone] (defaults to device default).
 *
 * Pure utility — no Android dependencies, fully unit-testable.
 */
object AttendanceDayKey {

    fun startOfDay(epochMillis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun startOfDay(date: Date, tz: TimeZone = TimeZone.getDefault()): Long =
        startOfDay(date.time, tz)

    fun startOfMonth(epochMillis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = epochMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun endOfMonthExclusive(epochMillis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = startOfMonth(epochMillis, tz)
            add(Calendar.MONTH, 1)
        }
        return cal.timeInMillis
    }

    fun daysInMonth(epochMillis: Long, tz: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
