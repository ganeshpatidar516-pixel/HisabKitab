package com.ganesh.hisabkitabpro.ui.staff

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object StaffFormatting {
    fun formatPaise(paise: Long, symbol: String = "\u20B9"): String {
        val sign = if (paise < 0) "-" else ""
        val abs = Math.abs(paise)
        val rupees = abs / 100
        val rem = abs % 100
        return "$sign$symbol ${"%,d".format(rupees)}.${"%02d".format(rem)}"
    }

    /** Best-effort parse user-input "1234.56" → paise. Returns null on garbage. */
    fun parseRupeesToPaise(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val rupees = cleaned.toBigDecimal().movePointRight(2)
            if (rupees.signum() < 0) null else rupees.toLong()
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun formatDate(epochMillis: Long): String =
        if (epochMillis <= 0L) "—"
        else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMillis))

    /** Format effective days. Whole numbers render without `.0`. */
    fun formatDays(days: Double): String =
        if (days % 1.0 == 0.0) "%.0f".format(days) else "%.1f".format(days)
}
