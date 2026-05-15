package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier

/**
 * Digital Native — neon hairlines, glowing accents, scanline grids. Uses BlurMaskFilter
 * to create authentic glow halos around the accent geometry rather than baked-in PNGs.
 */
internal object DigitalNativePainter : BusinessCardPainter {

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
        paintScanlines(canvas, bounds, palette.hairlineArgb)

        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentArgb
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.006f
            maskFilter = BlurMaskFilter(bounds.width() * 0.018f, BlurMaskFilter.Blur.NORMAL)
        }
        val crisp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accentArgb
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.0028f
        }

        when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_TR -> drawDiagonal(canvas, bounds, glow, crisp, true)
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_BL -> drawDiagonal(canvas, bounds, glow, crisp, false)
            BusinessCardLayoutBlueprint.AccentShape.LEFT_BAND -> drawNeonBand(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, vertical = true, atStart = true)
            BusinessCardLayoutBlueprint.AccentShape.RIGHT_BAND -> drawNeonBand(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, vertical = true, atStart = false)
            BusinessCardLayoutBlueprint.AccentShape.TOP_BAND -> drawNeonBand(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, vertical = false, atStart = true)
            BusinessCardLayoutBlueprint.AccentShape.BOTTOM_BAND -> drawNeonBand(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, vertical = false, atStart = false)
            BusinessCardLayoutBlueprint.AccentShape.CORNER_FRAME -> drawTechFrame(canvas, bounds, glow, crisp)
            BusinessCardLayoutBlueprint.AccentShape.HAIRLINE_GRID -> drawCircuit(canvas, bounds, crisp, palette.accentSecondaryArgb)
            BusinessCardLayoutBlueprint.AccentShape.RADIAL_GLOW -> drawHaloOrb(canvas, bounds, palette.accentArgb)
            BusinessCardLayoutBlueprint.AccentShape.FULL_BLEED -> drawHorizonGlow(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb)
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

    private fun paintScanlines(canvas: Canvas, bounds: RectF, hairline: Int) {
        val paint = Paint().apply {
            color = (hairline and 0x00FFFFFF) or 0x22000000
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val step = bounds.height() / 64f
        var y = bounds.top
        while (y <= bounds.bottom) {
            canvas.drawLine(bounds.left, y, bounds.right, y, paint)
            y += step
        }
    }

    private fun drawDiagonal(canvas: Canvas, bounds: RectF, glow: Paint, crisp: Paint, topRight: Boolean) {
        val path = Path()
        if (topRight) {
            path.moveTo(bounds.right - bounds.width() * 0.55f, bounds.top + bounds.height() * 0.10f)
            path.lineTo(bounds.right - bounds.width() * 0.05f, bounds.top + bounds.height() * 0.55f)
        } else {
            path.moveTo(bounds.left + bounds.width() * 0.05f, bounds.bottom - bounds.height() * 0.55f)
            path.lineTo(bounds.left + bounds.width() * 0.55f, bounds.bottom - bounds.height() * 0.10f)
        }
        canvas.drawPath(path, glow)
        canvas.drawPath(path, crisp)
    }

    private fun drawNeonBand(canvas: Canvas, bounds: RectF, c1: Int, c2: Int, vertical: Boolean, atStart: Boolean) {
        val band = if (vertical) {
            val w = bounds.width() * 0.045f
            if (atStart) RectF(bounds.left, bounds.top, bounds.left + w, bounds.bottom)
            else RectF(bounds.right - w, bounds.top, bounds.right, bounds.bottom)
        } else {
            val h = bounds.height() * 0.06f
            if (atStart) RectF(bounds.left, bounds.top, bounds.right, bounds.top + h)
            else RectF(bounds.left, bounds.bottom - h, bounds.right, bounds.bottom)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(band.left, band.top, band.right, band.bottom, intArrayOf(c1, c2, c1), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(band, paint)
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (c1 and 0x00FFFFFF) or 0x55000000
            maskFilter = BlurMaskFilter(bounds.width() * 0.02f, BlurMaskFilter.Blur.OUTER)
        }
        canvas.drawRect(band, glow)
    }

    private fun drawTechFrame(canvas: Canvas, bounds: RectF, glow: Paint, crisp: Paint) {
        val inset = bounds.width() * 0.035f
        val rect = RectF(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)
        val notch = bounds.width() * 0.04f
        val path = Path().apply {
            moveTo(rect.left + notch, rect.top)
            lineTo(rect.right, rect.top)
            lineTo(rect.right, rect.bottom - notch)
            lineTo(rect.right - notch, rect.bottom)
            lineTo(rect.left, rect.bottom)
            lineTo(rect.left, rect.top + notch)
            close()
        }
        canvas.drawPath(path, glow)
        canvas.drawPath(path, crisp)
    }

    private fun drawCircuit(canvas: Canvas, bounds: RectF, crisp: Paint, dotColor: Int) {
        val path = Path()
        val originY = bounds.bottom - bounds.height() * 0.15f
        path.moveTo(bounds.left + bounds.width() * 0.05f, originY)
        path.lineTo(bounds.left + bounds.width() * 0.30f, originY)
        path.lineTo(bounds.left + bounds.width() * 0.40f, originY - bounds.height() * 0.10f)
        path.lineTo(bounds.left + bounds.width() * 0.62f, originY - bounds.height() * 0.10f)
        path.lineTo(bounds.left + bounds.width() * 0.72f, originY)
        path.lineTo(bounds.right - bounds.width() * 0.05f, originY)
        canvas.drawPath(path, crisp)
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = dotColor; style = Paint.Style.FILL }
        val dotR = bounds.width() * 0.006f
        canvas.drawCircle(bounds.left + bounds.width() * 0.30f, originY, dotR, dot)
        canvas.drawCircle(bounds.left + bounds.width() * 0.40f, originY - bounds.height() * 0.10f, dotR, dot)
        canvas.drawCircle(bounds.left + bounds.width() * 0.62f, originY - bounds.height() * 0.10f, dotR, dot)
        canvas.drawCircle(bounds.left + bounds.width() * 0.72f, originY, dotR, dot)
    }

    private fun drawHaloOrb(canvas: Canvas, bounds: RectF, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = PainterToolkit.makeRadialGlow(bounds.right - bounds.width() * 0.18f, bounds.top + bounds.height() * 0.20f, bounds.width() * 0.5f, accent)
        }
        canvas.drawRect(bounds, paint)
    }

    private fun drawHorizonGlow(canvas: Canvas, bounds: RectF, c1: Int, c2: Int) {
        val band = RectF(bounds.left, bounds.bottom - bounds.height() * 0.40f, bounds.right, bounds.bottom)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(band.left, band.top, band.left, band.bottom, intArrayOf(0, (c1 and 0x00FFFFFF) or 0x44000000, c2), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(band, paint)
    }
}
