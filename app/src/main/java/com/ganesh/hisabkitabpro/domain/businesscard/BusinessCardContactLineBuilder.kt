package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Backward-compatible entry for the contact rail; implementation is [BusinessCardFieldRegistry].
 */
object BusinessCardContactLineBuilder {

    fun lines(profile: BusinessCardProfile): List<String> =
        BusinessCardFieldRegistry.orderedRailLines(profile)

    @Deprecated("Use BusinessCardUrlDisplay.shorten", ReplaceWith("BusinessCardUrlDisplay.shorten(url, maxLen)"))
    fun shortenUrl(url: String, maxLen: Int = 48): String = BusinessCardUrlDisplay.shorten(url, maxLen)
}
