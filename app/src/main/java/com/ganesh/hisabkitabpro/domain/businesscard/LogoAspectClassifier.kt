package com.ganesh.hisabkitabpro.domain.businesscard

/**
 * Pure classifier that buckets a logo bitmap's aspect ratio so the procedural
 * engine can choose the optimal anchoring (circle wells, square crops, landscape strips).
 *
 * A 12% tolerance band around 1.0 covers near-square brand marks; logos taller than 1.25:1
 * are treated as PORTRAIT and wider than 1.25:1 as LANDSCAPE.
 */
object LogoAspectClassifier {

    enum class LogoShape {
        CIRCLE,
        SQUARE,
        LANDSCAPE,
        PORTRAIT,
    }

    private const val SQUARE_TOLERANCE = 0.12

    fun classify(widthPx: Int, heightPx: Int, prefersCircular: Boolean = false): LogoShape {
        if (widthPx <= 0 || heightPx <= 0) return LogoShape.SQUARE
        val ratio = widthPx.toDouble() / heightPx.toDouble()
        val isSquareIsh = kotlin.math.abs(ratio - 1.0) <= SQUARE_TOLERANCE
        return when {
            isSquareIsh && prefersCircular -> LogoShape.CIRCLE
            isSquareIsh -> LogoShape.SQUARE
            ratio > 1.0 -> LogoShape.LANDSCAPE
            else -> LogoShape.PORTRAIT
        }
    }
}
