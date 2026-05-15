package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
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
 * Royal Signature — Amoled Black canvases dressed with metallic gold light bands.
 * Every accent ships with a true diagonal three-stop gradient (deep gold → bright gold
 * → deep gold) so the painted lines read as foil rather than flat fill.
 */
internal object RoyalSignaturePainter : BusinessCardPainter {

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

        val bg = Paint().apply { color = palette.backgroundArgb }
        canvas.drawRect(bounds, bg)

        if (palette.surfaceArgb != palette.backgroundArgb) {
            val surfaceInset = bounds.width() * 0.04f
            val surfaceRect = RectF(
                bounds.left + surfaceInset,
                bounds.top + surfaceInset,
                bounds.right - surfaceInset,
                bounds.bottom - surfaceInset,
            )
            val surfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.surfaceArgb
                PainterToolkit.softShadow(this, bounds.width() * 0.02f, bounds.width() * 0.005f, 0x66000000)
            }
            canvas.drawRoundRect(surfaceRect, bounds.width() * 0.02f, bounds.width() * 0.02f, surfacePaint)
        }

        drawGoldAccent(canvas, bounds, blueprint, palette.accentArgb, palette.accentSecondaryArgb)
        drawCornerCrests(canvas, bounds, blueprint, palette.accentArgb)

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

    private fun drawGoldAccent(canvas: Canvas, bounds: RectF, blueprint: BusinessCardLayoutBlueprint, gold: Int, deepGold: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_TR -> {
                val path = Path().apply {
                    moveTo(bounds.right - bounds.width() * 0.55f, bounds.top)
                    lineTo(bounds.right, bounds.top)
                    lineTo(bounds.right, bounds.top + bounds.height() * 0.55f)
                    close()
                }
                paint.shader = LinearGradient(bounds.right, bounds.top, bounds.right - bounds.width() * 0.5f, bounds.top + bounds.height() * 0.5f, intArrayOf(gold, 0xFFF5DC8B.toInt(), deepGold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                canvas.drawPath(path, paint)
            }
            BusinessCardLayoutBlueprint.AccentShape.DIAGONAL_BL -> {
                val path = Path().apply {
                    moveTo(bounds.left, bounds.bottom)
                    lineTo(bounds.left + bounds.width() * 0.55f, bounds.bottom)
                    lineTo(bounds.left, bounds.bottom - bounds.height() * 0.55f)
                    close()
                }
                paint.shader = LinearGradient(bounds.left, bounds.bottom, bounds.left + bounds.width() * 0.5f, bounds.bottom - bounds.height() * 0.5f, intArrayOf(gold, 0xFFF5DC8B.toInt(), deepGold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                canvas.drawPath(path, paint)
            }
            BusinessCardLayoutBlueprint.AccentShape.LEFT_BAND -> drawBand(canvas, bounds, true, true, gold, deepGold, paint)
            BusinessCardLayoutBlueprint.AccentShape.RIGHT_BAND -> drawBand(canvas, bounds, true, false, gold, deepGold, paint)
            BusinessCardLayoutBlueprint.AccentShape.TOP_BAND -> drawBand(canvas, bounds, false, true, gold, deepGold, paint)
            BusinessCardLayoutBlueprint.AccentShape.BOTTOM_BAND -> drawBand(canvas, bounds, false, false, gold, deepGold, paint)
            BusinessCardLayoutBlueprint.AccentShape.CORNER_FRAME -> drawCornerFrame(canvas, bounds, gold, deepGold)
            BusinessCardLayoutBlueprint.AccentShape.HAIRLINE_GRID -> drawHairlineGrid(canvas, bounds, gold)
            BusinessCardLayoutBlueprint.AccentShape.RADIAL_GLOW -> drawRadialFoil(canvas, bounds, gold, deepGold)
            BusinessCardLayoutBlueprint.AccentShape.FULL_BLEED -> drawFullFoil(canvas, bounds, gold, deepGold, paint)
        }
    }

    private fun drawBand(canvas: Canvas, bounds: RectF, vertical: Boolean, atStart: Boolean, gold: Int, deepGold: Int, paint: Paint) {
        val band = if (vertical) {
            val w = bounds.width() * 0.075f
            if (atStart) RectF(bounds.left, bounds.top, bounds.left + w, bounds.bottom)
            else RectF(bounds.right - w, bounds.top, bounds.right, bounds.bottom)
        } else {
            val h = bounds.height() * 0.12f
            if (atStart) RectF(bounds.left, bounds.top, bounds.right, bounds.top + h)
            else RectF(bounds.left, bounds.bottom - h, bounds.right, bounds.bottom)
        }
        paint.shader = LinearGradient(band.left, band.top, band.right, band.bottom, intArrayOf(deepGold, gold, deepGold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(band, paint)
    }

    private fun drawCornerFrame(canvas: Canvas, bounds: RectF, gold: Int, deepGold: Int) {
        val stroke = bounds.width() * 0.005f
        val inset = bounds.width() * 0.05f
        val rect = RectF(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, gold, deepGold, Shader.TileMode.CLAMP)
        }
        val cornerLen = bounds.width() * 0.08f
        canvas.drawLine(rect.left, rect.top, rect.left + cornerLen, rect.top, paint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLen, paint)
        canvas.drawLine(rect.right - cornerLen, rect.top, rect.right, rect.top, paint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLen, paint)
        canvas.drawLine(rect.left, rect.bottom - cornerLen, rect.left, rect.bottom, paint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLen, rect.bottom, paint)
        canvas.drawLine(rect.right - cornerLen, rect.bottom, rect.right, rect.bottom, paint)
        canvas.drawLine(rect.right, rect.bottom - cornerLen, rect.right, rect.bottom, paint)
    }

    private fun drawHairlineGrid(canvas: Canvas, bounds: RectF, gold: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (gold and 0x00FFFFFF) or 0x33000000
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.0015f
        }
        val step = bounds.width() / 16f
        var x = bounds.left
        while (x < bounds.right) {
            canvas.drawLine(x, bounds.top, x, bounds.bottom, paint)
            x += step
        }
        var y = bounds.top
        while (y < bounds.bottom) {
            canvas.drawLine(bounds.left, y, bounds.right, y, paint)
            y += step
        }
    }

    private fun drawRadialFoil(canvas: Canvas, bounds: RectF, gold: Int, deepGold: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = PainterToolkit.makeRadialGlow(bounds.right - bounds.width() * 0.18f, bounds.top + bounds.height() * 0.22f, bounds.width() * 0.42f, gold)
        canvas.drawRect(bounds, paint)
        paint.shader = PainterToolkit.makeRadialGlow(bounds.left + bounds.width() * 0.16f, bounds.bottom - bounds.height() * 0.20f, bounds.width() * 0.30f, deepGold)
        canvas.drawRect(bounds, paint)
    }

    private fun drawFullFoil(canvas: Canvas, bounds: RectF, gold: Int, deepGold: Int, paint: Paint) {
        paint.shader = LinearGradient(bounds.left, bounds.top, bounds.right, bounds.bottom, intArrayOf(deepGold, gold, deepGold), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(bounds, paint)
    }

    private fun drawCornerCrests(canvas: Canvas, bounds: RectF, blueprint: BusinessCardLayoutBlueprint, gold: Int) {
        // Tiny crest dot signature in the corner opposite the QR — keeps each card identifiable.
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            style = Paint.Style.FILL
        }
        val r = bounds.width() * 0.006f
        val cx: Float
        val cy: Float
        when (blueprint.qrPlacement) {
            BusinessCardLayoutBlueprint.QrPlacement.BOTTOM_RIGHT, BusinessCardLayoutBlueprint.QrPlacement.INSET_RIGHT -> {
                cx = bounds.left + bounds.width() * 0.05f
                cy = bounds.top + bounds.height() * 0.07f
            }
            else -> {
                cx = bounds.right - bounds.width() * 0.05f
                cy = bounds.top + bounds.height() * 0.07f
            }
        }
        canvas.drawCircle(cx, cy, r, paint)
        canvas.drawCircle(cx + r * 3.2f, cy, r * 0.65f, paint)
        canvas.drawCircle(cx - r * 3.2f, cy, r * 0.65f, paint)
    }
}
