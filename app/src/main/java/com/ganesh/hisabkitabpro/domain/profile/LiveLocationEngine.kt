package com.ganesh.hisabkitabpro.domain.profile

import java.util.Locale

/**
 * Small, dependency-free location layer that stores precise coordinates and produces
 * a Google Maps link for downstream documents. It is intentionally independent of
 * billing/ledger code so profile location can evolve without touching money flows.
 */
object LiveLocationEngine {

    fun buildMapLink(latitude: Double?, longitude: Double?): String {
        if (latitude == null || longitude == null) return ""
        if (!latitude.isFinite() || !longitude.isFinite()) return ""
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return ""
        return String.format(
            Locale.US,
            "https://www.google.com/maps/search/?api=1&query=%.7f,%.7f",
            latitude,
            longitude,
        )
    }

    fun parseCoordinate(input: String, min: Double, max: Double): Double? =
        input.trim().toDoubleOrNull()?.takeIf { it.isFinite() && it in min..max }

    fun isLocked(latitude: Double?, longitude: Double?, lockedAt: Long): Boolean =
        buildMapLink(latitude, longitude).isNotBlank() && lockedAt > 0L
}
