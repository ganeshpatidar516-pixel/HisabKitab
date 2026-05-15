package com.ganesh.hisabkitabpro.domain.support

import java.time.LocalTime
import java.time.ZoneId

/**
 * Live human support window for Help & WhatsApp chat (device local time).
 * Inclusive 10:00 through 17:00 (5:00 PM).
 */
object SupportBusinessHours {
    private val OPEN = LocalTime.of(10, 0)
    private val CLOSE = LocalTime.of(17, 0)

    fun isWithinLiveSupportNow(zoneId: ZoneId = ZoneId.systemDefault()): Boolean {
        val now = LocalTime.now(zoneId)
        return !now.isBefore(OPEN) && !now.isAfter(CLOSE)
    }
}
