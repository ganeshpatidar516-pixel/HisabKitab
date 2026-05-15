package com.ganesh.hisabkitabpro.ui.transactions

/**
 * Heuristic GST % suggestion from HSN digits (India-style chapter headings).
 * **Not legal/tax advice** — rates vary by exact HSN, exemptions, notifications, and country.
 * UI must always allow manual override; we only suggest when the user has left GST at 0.
 */
internal object HsnGstLookup {

    private val allowedSlabs = setOf("0", "5", "12", "18", "28")

    /**
     * Returns one of [0,5,12,18,28] or null if HSN is too short / non-numeric to guess.
     */
    fun suggestGstPercentForHsn(hsnRaw: String): String? {
        val digits = hsnRaw.filter { it.isDigit() }
        if (digits.length < 4) return null
        val chapter = digits.take(2).toIntOrNull() ?: return null
        val raw = suggestByChapter(chapter)
        return raw.takeIf { it in allowedSlabs }
    }

    /** Two-digit HSN chapter (01–99) → indicative slab % string. */
    private fun suggestByChapter(ch: Int): String = when (ch) {
        in 1..5 -> "5"
        in 6..15 -> "5"
        in 16..17 -> "12"
        in 18..24 -> "12"
        25, 26 -> "5"
        27 -> "18"
        28, 29 -> "18"
        30 -> "12"
        in 31..37 -> "18"
        38 -> "12"
        in 39..43 -> "18"
        in 44..46 -> "18"
        in 47..49 -> "12"
        in 50..66 -> "5"
        67 -> "18"
        in 68..70 -> "18"
        71 -> "18"
        in 72..85 -> "18"
        in 86..89 -> "28"
        in 90..92 -> "18"
        93 -> "28"
        in 94..96 -> "18"
        97 -> "12"
        in 98..99 -> "18"
        else -> "18"
    }
}
