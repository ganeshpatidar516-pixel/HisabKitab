package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Pure utility that derives layout metrics from the Golden Ratio (φ = 1.618).
 *
 * Given a card width (in pixels or any consistent unit) the grid yields:
 *  - The φ-divided primary content band and accent band.
 *  - A safe inner padding (1/φ³ of the width).
 *  - A typographic baseline rhythm based on (1/φ) of the band height.
 *
 * The grid is fully unit-testable; it has no Android dependencies.
 */
object GoldenRatioGrid {

    const val PHI: Double = 1.6180339887498949

    /** Standard ISO 7810 ID-1 (business card) aspect ratio is ~1.586; we render at φ for stronger optics. */
    const val CARD_ASPECT_RATIO: Double = PHI

    data class Metrics(
        val widthPx: Float,
        val heightPx: Float,
        val safePadding: Float,
        val majorBandWidth: Float,
        val minorBandWidth: Float,
        val titleSizePx: Float,
        val subtitleSizePx: Float,
        val bodySizePx: Float,
        val microSizePx: Float,
        val baselineRhythm: Float,
    )

    fun compute(widthPx: Float, heightPx: Float = (widthPx / PHI).toFloat()): Metrics {
        require(widthPx > 0f) { "widthPx must be positive" }
        require(heightPx > 0f) { "heightPx must be positive" }

        val majorBand = (widthPx / PHI).toFloat()
        val minorBand = widthPx - majorBand
        val safePadding = (widthPx / (PHI * PHI * PHI)).toFloat()

        val titleSize = (heightPx / (PHI * PHI)).toFloat()
        val subtitleSize = (titleSize / PHI).toFloat()
        val bodySize = (subtitleSize / PHI).toFloat()
        val microSize = (bodySize / PHI).toFloat()
        val rhythm = (bodySize * PHI).toFloat()

        return Metrics(
            widthPx = widthPx,
            heightPx = heightPx,
            safePadding = safePadding,
            majorBandWidth = majorBand,
            minorBandWidth = minorBand,
            titleSizePx = titleSize,
            subtitleSizePx = subtitleSize,
            bodySizePx = bodySize,
            microSizePx = microSize,
            baselineRhythm = rhythm,
        )
    }
}
