package com.ganesh.hisabkitabpro.domain.businesscard.layout

/**
 * Axis-aligned rectangle in layout space (same units as card canvas: px at export DPI).
 * JVM-testable replacement for [android.graphics.RectF] in pure geometry paths.
 */
data class LayoutRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
    fun centerX(): Float = (left + right) * 0.5f
    fun centerY(): Float = (top + bottom) * 0.5f

    /** True when this rectangle has positive area and overlaps [other] (edges touching count as overlap). */
    fun intersects(other: LayoutRect): Boolean =
        left < other.right && right > other.left && top < other.bottom && bottom > other.top

    companion object {
        fun fromLtrb(l: Float, t: Float, r: Float, b: Float) = LayoutRect(l, t, r, b)
    }
}
