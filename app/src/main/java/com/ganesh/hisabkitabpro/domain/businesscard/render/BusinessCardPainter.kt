package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardCategory
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier

/**
 * Single contract every category renderer implements. The painter is pure with respect
 * to the canvas it receives — no global state, no caching — so the same painter drives
 * Compose previews and the off-screen 300 DPI bitmap exporter without behavioural drift.
 */
interface BusinessCardPainter {
    fun paint(
        canvas: Canvas,
        bounds: RectF,
        variation: BusinessCardVariation,
        profile: BusinessCardProfile,
        logo: Bitmap?,
        qr: Bitmap?,
        logoShape: LogoAspectClassifier.LogoShape,
    )
}

object BusinessCardPainterRegistry {
    fun painterFor(category: BusinessCardCategory): BusinessCardPainter = when (category) {
        BusinessCardCategory.ROYAL_SIGNATURE -> RoyalSignaturePainter
        BusinessCardCategory.CORPORATE_ELITE -> CorporateElitePainter
        BusinessCardCategory.MODERN_ABSTRACT -> ModernAbstractPainter
        BusinessCardCategory.ECO_ORGANIC -> EcoOrganicPainter
        BusinessCardCategory.DIGITAL_NATIVE -> DigitalNativePainter
    }
}
