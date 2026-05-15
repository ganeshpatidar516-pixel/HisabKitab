package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier

/**
 * Corporate Elite — opinionated minimalism. Heavy negative space, single razor band,
 * never more than one accent shape per card.
 */
internal object CorporateElitePainter : BusinessCardPainter {

    override fun paint(
        canvas: Canvas,
        bounds: RectF,
        variation: BusinessCardVariation,
        profile: BusinessCardProfile,
        logo: Bitmap?,
        qr: Bitmap?,
        logoShape: LogoAspectClassifier.LogoShape,
    ) {
        val palette = variation.palette
        val blueprint = variation.blueprint

        canvas.drawRect(bounds, Paint().apply { color = palette.backgroundArgb })

        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accentArgb; style = Paint.Style.FILL }
        val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.hairlineArgb
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.0028f
        }

        when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.LEFT_BAND -> canvas.drawRect(RectF(bounds.left, bounds.top, bounds.left + bounds.width() * 0.022f, bounds.bottom), accent)
            BusinessCardLayoutBlueprint.AccentShape.RIGHT_BAND -> canvas.drawRect(RectF(bounds.right - bounds.width() * 0.022f, bounds.top, bounds.right, bounds.bottom), accent)
            BusinessCardLayoutBlueprint.AccentShape.TOP_BAND -> canvas.drawRect(RectF(bounds.left, bounds.top, bounds.right, bounds.top + bounds.height() * 0.04f), accent)
            BusinessCardLayoutBlueprint.AccentShape.BOTTOM_BAND -> canvas.drawRect(RectF(bounds.left, bounds.bottom - bounds.height() * 0.04f, bounds.right, bounds.bottom), accent)
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_TR -> drawDiagonalRule(canvas, bounds, accent, true)
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_BL -> drawDiagonalRule(canvas, bounds, accent, false)
            BusinessCardLayoutBlueprint.AccentShape.HAIRLINE_GRID -> drawSparseGrid(canvas, bounds, hairline)
            BusinessCardLayoutBlueprint.AccentShape.CORNER_FRAME -> drawCornerNotation(canvas, bounds, hairline)
            BusinessCardLayoutBlueprint.AccentShape.RADIAL_GLOW -> drawSoftCenter(canvas, bounds, palette.accentArgb)
            BusinessCardLayoutBlueprint.AccentShape.FULL_BLEED -> {
                accent.alpha = 32
                canvas.drawRect(bounds, accent)
            }
        }

        CardLayoutCompositor.composeBody(
            canvas = canvas,
            bounds = bounds,
            blueprint = blueprint,
            palette = palette,
            typography = variation.typography,
            profile = profile,
            logo = logo,
            qr = qr,
            logoShape = logoShape,
        )
    }

    private fun drawDiagonalRule(canvas: Canvas, bounds: RectF, paint: Paint, topRight: Boolean) {
        val style = Paint(paint).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.004f
        }
        if (topRight) {
            canvas.drawLine(bounds.right - bounds.width() * 0.30f, bounds.top, bounds.right, bounds.top + bounds.height() * 0.30f, style)
        } else {
            canvas.drawLine(bounds.left, bounds.bottom - bounds.height() * 0.30f, bounds.left + bounds.width() * 0.30f, bounds.bottom, style)
        }
    }

    private fun drawSparseGrid(canvas: Canvas, bounds: RectF, hairline: Paint) {
        val step = bounds.width() / 24f
        var x = bounds.left + step
        while (x < bounds.right) {
            canvas.drawLine(x, bounds.bottom - bounds.height() * 0.05f, x, bounds.bottom, hairline)
            x += step
        }
    }

    private fun drawCornerNotation(canvas: Canvas, bounds: RectF, hairline: Paint) {
        val len = bounds.width() * 0.05f
        val pad = bounds.width() * 0.04f
        canvas.drawLine(bounds.left + pad, bounds.top + pad, bounds.left + pad + len, bounds.top + pad, hairline)
        canvas.drawLine(bounds.left + pad, bounds.top + pad, bounds.left + pad, bounds.top + pad + len, hairline)
        canvas.drawLine(bounds.right - pad - len, bounds.bottom - pad, bounds.right - pad, bounds.bottom - pad, hairline)
        canvas.drawLine(bounds.right - pad, bounds.bottom - pad - len, bounds.right - pad, bounds.bottom - pad, hairline)
    }

    private fun drawSoftCenter(canvas: Canvas, bounds: RectF, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = PainterToolkit.makeRadialGlow(bounds.centerX(), bounds.centerY(), bounds.width() * 0.5f, (accent and 0x00FFFFFF) or 0x14000000)
        }
        canvas.drawRect(bounds, paint)
    }
}
