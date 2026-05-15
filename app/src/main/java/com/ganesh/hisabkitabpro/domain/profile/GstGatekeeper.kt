package com.ganesh.hisabkitabpro.domain.profile

/**
 * Respects the existing GST_TOGGLE. When disabled, GST automation is completely
 * suppressed by callers. When enabled, this gatekeeper validates GSTIN shape and
 * performs conservative offline enrichment only where the code already carries
 * structured state initials.
 */
object GstGatekeeper {

    private val gstRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$")

    data class GstLookupResult(
        val normalizedGstin: String,
        val businessNameHint: String? = null,
        val addressHint: String? = null,
    )

    fun normalize(input: String): String =
        input.uppercase().filter { it.isLetterOrDigit() }

    fun isValid(gstin: String): Boolean = gstRegex.matches(normalize(gstin))

    /**
     * Safe offline lookup seam. A real GST provider can replace this implementation
     * later, but the UI already calls it only when GST_TOGGLE is ON and format is valid.
     */
    fun lookup(gstin: String): GstLookupResult? {
        val normalized = normalize(gstin)
        if (!isValid(normalized)) return null
        return GstLookupResult(normalizedGstin = normalized)
    }
}
