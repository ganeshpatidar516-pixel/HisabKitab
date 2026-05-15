package com.ganesh.hisabkitabpro.domain.profile

import com.ganesh.hisabkitabpro.domain.model.BusinessProfile

/**
 * Single source for **merchant identity footers** on WhatsApp/SMS and related surfaces.
 * Composes only context-appropriate lines (no raw Google Maps URLs in customer-visible text).
 */
object ProfileMapFooter {

    /**
     * Rich optional footer for reminders and share text: phone, UPI, email, site, address
     * headline, and a short location status (never the raw map URL).
     */
    fun mapFooter(profile: BusinessProfile?): String? {
        if (profile == null) return null
        val lines = mutableListOf<String>()
        profile.phone.trim().takeIf { it.isNotBlank() }?.let { lines += "📞 $it" }
        profile.upiId.trim().takeIf { it.isNotBlank() }?.let { lines += "UPI: $it" }
        profile.email.trim().takeIf { it.isNotBlank() }?.let { lines += "✉ $it" }
        profile.websiteUrl.trim().takeIf { it.isNotBlank() }?.let { lines += "🌐 $it" }
        val wa = (profile.whatsAppBusinessUrl ?: "").trim()
        if (wa.isNotBlank()) {
            val short = if (wa.length > 48) wa.take(48) + "…" else wa
            lines += "WhatsApp: $short"
        }
        addressHeadline(profile)?.let { lines += "📍 $it" }
        locationStatusLine(profile)?.let { lines += it }
        if (lines.isEmpty()) return null
        return lines.joinToString("\n")
    }

    /**
     * One line for invoice PDF headers / footers (address headline preferred; else status).
     */
    fun invoiceLocationCaption(profile: BusinessProfile?): String? {
        if (profile == null) return null
        addressHeadline(profile)?.let { return "📍 $it" }
        return locationStatusLine(profile)
    }

    /**
     * Printed business card contact rail (`M  …`) — short map status, never a raw URL.
     */
    fun cardLocationRailLine(
        mapLink: String,
        latitude: Double? = null,
        longitude: Double? = null,
        locationLockedAt: Long = 0L,
    ): String? {
        val hasLink = mapLink.trim().isNotBlank()
        if (!hasLink) return null
        val locked = LiveLocationEngine.isLocked(latitude, longitude, locationLockedAt)
        val text = if (locked) "Map location verified" else "Map location on file"
        return "M  $text"
    }

    private fun addressHeadline(profile: BusinessProfile): String? =
        profile.address.trim().lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }

    private fun locationStatusLine(profile: BusinessProfile): String? {
        val hasLink = profile.mapLink.trim().isNotBlank()
        val locked = LiveLocationEngine.isLocked(
            profile.latitude,
            profile.longitude,
            profile.locationLockedAt,
        )
        return when {
            locked && hasLink -> "📍 Map location verified"
            hasLink -> "📍 Map location on file"
            else -> null
        }
    }
}
