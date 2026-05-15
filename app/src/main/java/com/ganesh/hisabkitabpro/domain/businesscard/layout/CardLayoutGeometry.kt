package com.ganesh.hisabkitabpro.domain.businesscard.layout

import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import kotlin.math.max
import kotlin.math.min

private data class TextBand(
    var left: Float,
    var top: Float,
    var right: Float,
    var bottom: Float,
) {
    fun resetToInnerPadding(bounds: LayoutRect, padding: Float) {
        left = bounds.left + padding
        top = bounds.top + padding
        right = bounds.right - padding
        bottom = bounds.bottom - padding
    }
}

private fun applyLogoInsets(
    band: TextBand,
    bounds: LayoutRect,
    padding: Float,
    logoSlot: LayoutRect?,
    anchor: BusinessCardLayoutBlueprint.LogoAnchor,
    textAlignment: BusinessCardLayoutBlueprint.TextAlignment,
) {
    logoSlot ?: return
    val slot = logoSlot
    val minTextBandHeight = bounds.height() * 0.12f
    val looseBottomPad = padding * 0.12f
    val looseTopPad = padding * 0.12f
    when (anchor) {
        BusinessCardLayoutBlueprint.LogoAnchor.TOP_LEFT,
        BusinessCardLayoutBlueprint.LogoAnchor.TOP_RIGHT,
        BusinessCardLayoutBlueprint.LogoAnchor.TOP_CENTER -> {
            if (slot.bottom > band.top) {
                val idealTop = slot.bottom + padding * 0.5f
                if (idealTop > band.bottom - minTextBandHeight) {
                    band.bottom = bounds.bottom - looseBottomPad
                }
                band.top = max(band.top, idealTop).coerceAtMost(band.bottom - minTextBandHeight)
            }
        }
        BusinessCardLayoutBlueprint.LogoAnchor.BOTTOM_LEFT,
        BusinessCardLayoutBlueprint.LogoAnchor.BOTTOM_RIGHT -> {
            if (slot.top < band.bottom) {
                val idealBottom = slot.top - padding * 0.5f
                if (idealBottom < band.top + minTextBandHeight) {
                    band.top = bounds.top + looseTopPad
                }
                band.bottom = min(band.bottom, idealBottom).coerceAtLeast(band.top + minTextBandHeight)
            }
        }
        BusinessCardLayoutBlueprint.LogoAnchor.CENTER_LEFT -> {
            band.left = max(band.left, slot.right + padding * 0.45f)
        }
        BusinessCardLayoutBlueprint.LogoAnchor.CENTER_RIGHT -> {
            band.right = min(band.right, slot.left - padding * 0.45f)
        }
        BusinessCardLayoutBlueprint.LogoAnchor.CENTER -> {
            val bandPad = padding * 0.45f
            if (textAlignment == BusinessCardLayoutBlueprint.TextAlignment.CENTER) {
                val hGap = padding * 0.26f
                band.left = max(band.left, slot.right + hGap)
                band.right = min(band.right, slot.left - hGap)
                if (band.left >= band.right - 2f) {
                    band.left = bounds.left + padding
                    band.right = bounds.right - padding
                    val idealTop = slot.bottom + bandPad * 0.55f
                    if (idealTop < band.bottom - minTextBandHeight) {
                        band.bottom = bounds.bottom - looseBottomPad
                        band.top = max(band.top, idealTop).coerceAtMost(band.bottom - minTextBandHeight)
                    } else {
                        val idealBottom = slot.top - bandPad * 0.55f
                        band.top = bounds.top + looseTopPad
                        band.bottom = min(band.bottom, idealBottom).coerceAtLeast(band.top + minTextBandHeight)
                    }
                }
            } else if (slot.bottom + bandPad < band.bottom) {
                val idealTop = slot.bottom + bandPad
                if (idealTop > band.bottom - minTextBandHeight) {
                    band.bottom = bounds.bottom - looseBottomPad
                }
                band.top = max(band.top, idealTop).coerceAtMost(band.bottom - minTextBandHeight)
            } else if (slot.top - bandPad > band.top) {
                val idealBottom = slot.top - bandPad
                if (idealBottom < band.top + minTextBandHeight) {
                    band.top = bounds.top + looseTopPad
                }
                band.bottom = min(band.bottom, idealBottom).coerceAtLeast(band.top + minTextBandHeight)
            }
        }
    }
}

/**
 * Pure layout math for the business card body composer. No Android types — safe for JVM unit tests
 * and reusable from [com.ganesh.hisabkitabpro.domain.businesscard.render.CardLayoutCompositor].
 */
object CardLayoutGeometry {

    const val LOGO_FRACTION_OF_WIDTH: Float = 0.18f
    const val QR_FRACTION_OF_WIDTH: Float = 0.20f
    /** Floor as a fraction of card width so QR stays scannable after downscale. */
    const val QR_MIN_FRACTION_OF_WIDTH: Float = 0.168f
    /** Cap QR module relative to the shorter card edge so it stays on-card. */
    const val QR_MAX_FRACTION_OF_MIN_EDGE: Float = 0.28f

    fun logoSideLength(cardWidthPx: Float): Float = cardWidthPx * LOGO_FRACTION_OF_WIDTH

    fun qrSideLength(cardWidthPx: Float, cardHeightPx: Float): Float {
        val w = cardWidthPx
        val h = cardHeightPx
        val base = max(w * QR_FRACTION_OF_WIDTH, w * QR_MIN_FRACTION_OF_WIDTH)
        return base.coerceAtMost(min(w, h) * QR_MAX_FRACTION_OF_MIN_EDGE)
    }

    fun logoSlot(
        bounds: LayoutRect,
        anchor: BusinessCardLayoutBlueprint.LogoAnchor,
        padding: Float,
        sideLength: Float,
    ): LayoutRect {
        val w = sideLength
        val h = sideLength
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        return when (anchor) {
            BusinessCardLayoutBlueprint.LogoAnchor.TOP_LEFT -> LayoutRect(
                bounds.left + padding, bounds.top + padding,
                bounds.left + padding + w, bounds.top + padding + h,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.TOP_RIGHT -> LayoutRect(
                bounds.right - padding - w, bounds.top + padding,
                bounds.right - padding, bounds.top + padding + h,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.TOP_CENTER -> LayoutRect(
                cx - w / 2f, bounds.top + padding,
                cx + w / 2f, bounds.top + padding + h,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.CENTER_LEFT -> LayoutRect(
                bounds.left + padding, cy - h / 2f,
                bounds.left + padding + w, cy + h / 2f,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.CENTER_RIGHT -> LayoutRect(
                bounds.right - padding - w, cy - h / 2f,
                bounds.right - padding, cy + h / 2f,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.BOTTOM_LEFT -> LayoutRect(
                bounds.left + padding, bounds.bottom - padding - h,
                bounds.left + padding + w, bounds.bottom - padding,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.BOTTOM_RIGHT -> LayoutRect(
                bounds.right - padding - w, bounds.bottom - padding - h,
                bounds.right - padding, bounds.bottom - padding,
            )
            BusinessCardLayoutBlueprint.LogoAnchor.CENTER -> LayoutRect(
                cx - w / 2f, cy - h / 2f,
                cx + w / 2f, cy + h / 2f,
            )
        }
    }

    fun qrSlot(
        bounds: LayoutRect,
        placement: BusinessCardLayoutBlueprint.QrPlacement,
        padding: Float,
        sideLength: Float,
    ): LayoutRect {
        val w = sideLength
        val h = sideLength
        val cy = bounds.centerY()
        return when (placement) {
            BusinessCardLayoutBlueprint.QrPlacement.TOP_LEFT -> LayoutRect(
                bounds.left + padding, bounds.top + padding,
                bounds.left + padding + w, bounds.top + padding + h,
            )
            BusinessCardLayoutBlueprint.QrPlacement.TOP_RIGHT -> LayoutRect(
                bounds.right - padding - w, bounds.top + padding,
                bounds.right - padding, bounds.top + padding + h,
            )
            BusinessCardLayoutBlueprint.QrPlacement.BOTTOM_LEFT -> LayoutRect(
                bounds.left + padding, bounds.bottom - padding - h,
                bounds.left + padding + w, bounds.bottom - padding,
            )
            BusinessCardLayoutBlueprint.QrPlacement.BOTTOM_RIGHT -> LayoutRect(
                bounds.right - padding - w, bounds.bottom - padding - h,
                bounds.right - padding, bounds.bottom - padding,
            )
            BusinessCardLayoutBlueprint.QrPlacement.INSET_RIGHT -> LayoutRect(
                bounds.right - padding - w, cy - h / 2f,
                bounds.right - padding, cy + h / 2f,
            )
            BusinessCardLayoutBlueprint.QrPlacement.INSET_LEFT -> LayoutRect(
                bounds.left + padding, cy - h / 2f,
                bounds.left + padding + w, cy + h / 2f,
            )
        }
    }

    /**
     * Safe rectangle for typography (title, tagline, owner, contacts) after reserving logo and QR.
     */
    fun computeTextSafeRect(
        bounds: LayoutRect,
        padding: Float,
        logoSlot: LayoutRect?,
        qrSlot: LayoutRect?,
        blueprint: BusinessCardLayoutBlueprint,
    ): LayoutRect {
        val band = TextBand(
            left = bounds.left + padding,
            top = bounds.top + padding,
            right = bounds.right - padding,
            bottom = bounds.bottom - padding,
        )

        applyLogoInsets(band, bounds, padding, logoSlot, blueprint.logoAnchor, blueprint.textAlignment)

        qrSlot?.let { slot ->
            val cleared = applyHorizontalQrClearance(
                bounds, padding, slot, blueprint.textAlignment, band.top, band.bottom, band.left, band.right,
            )
            band.left = cleared.first
            band.right = cleared.second
        }

        if (band.left >= band.right - 2f) {
            band.left = bounds.left + padding
            band.right = bounds.right - padding
            applyLogoInsets(band, bounds, padding, logoSlot, blueprint.logoAnchor, blueprint.textAlignment)
            qrSlot?.let { slot ->
                val cleared = applyHorizontalQrClearance(
                    bounds, padding, slot, blueprint.textAlignment, band.top, band.bottom, band.left, band.right,
                )
                band.left = cleared.first
                band.right = cleared.second
            }
        }
        if (band.top >= band.bottom - 2f) {
            band.resetToInnerPadding(bounds, padding)
            applyLogoInsets(band, bounds, padding, logoSlot, blueprint.logoAnchor, blueprint.textAlignment)
            qrSlot?.let { slot ->
                val cleared = applyHorizontalQrClearance(
                    bounds, padding, slot, blueprint.textAlignment, band.top, band.bottom, band.left, band.right,
                )
                band.left = cleared.first
                band.right = cleared.second
            }
            if (band.top >= band.bottom - 2f) {
                band.top = bounds.top + padding
                band.bottom = bounds.bottom - padding
            }
        }
        return LayoutRect(band.left, band.top, band.right, band.bottom)
    }

    /**
     * Shrinks [left],[right] so the text band clears [slot] horizontally when it overlaps
     * the current vertical band. Used once after logo inset and again if horizontal insets inverted.
     */
    private fun applyHorizontalQrClearance(
        bounds: LayoutRect,
        padding: Float,
        slot: LayoutRect,
        textAlignment: BusinessCardLayoutBlueprint.TextAlignment,
        top: Float,
        bottom: Float,
        left: Float,
        right: Float,
    ): Pair<Float, Float> {
        var l = left
        var r = right
        val overlapsVertically = slot.top < bottom && slot.bottom > top
        if (!overlapsVertically) return l to r
        if (!(slot.right > l && slot.left < r)) return l to r
        val qrCenterX = slot.centerX()
        val cardMidX = bounds.centerX()
        fun clearanceForRightTrim(): Float {
            var gap = padding * 0.5f
            val minGap = padding * 0.16f
            while (gap >= minGap - 0.01f) {
                val candidate = min(r, slot.left - gap)
                if (candidate > l + 6f) return gap
                gap -= padding * 0.06f
            }
            return minGap
        }
        fun clearanceForLeftTrim(): Float {
            var gap = padding * 0.5f
            val minGap = padding * 0.16f
            while (gap >= minGap - 0.01f) {
                val candidate = max(l, slot.right + gap)
                if (candidate < r - 6f) return gap
                gap -= padding * 0.06f
            }
            return minGap
        }
        when (textAlignment) {
            BusinessCardLayoutBlueprint.TextAlignment.LEAD_LEFT -> {
                when {
                    qrCenterX < cardMidX -> l = max(l, slot.right + clearanceForLeftTrim())
                    qrCenterX > cardMidX -> r = min(r, slot.left - clearanceForRightTrim())
                    else -> Unit
                }
            }
            BusinessCardLayoutBlueprint.TextAlignment.LEAD_RIGHT -> {
                when {
                    qrCenterX > cardMidX -> r = min(r, slot.left - clearanceForRightTrim())
                    qrCenterX < cardMidX -> l = max(l, slot.right + clearanceForLeftTrim())
                    else -> Unit
                }
            }
            BusinessCardLayoutBlueprint.TextAlignment.CENTER -> {
                when {
                    qrCenterX < cardMidX -> l = max(l, slot.right + clearanceForLeftTrim())
                    qrCenterX > cardMidX -> r = min(r, slot.left - clearanceForRightTrim())
                    else -> Unit
                }
            }
        }
        return l to r
    }
}
