package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier
import kotlin.math.cos
import kotlin.math.sin

/**
 * Eco Organic — paper-soft palette with hand-drawn quadratic curves and procedurally
 * placed grain dots. Generates the appearance of recycled stock without using any
 * raster textures.
 */
internal object EcoOrganicPainter : BusinessCardPainter {

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
        drawGrain(canvas, bounds, palette.muteArgb, variation.seed)

        when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.TOP_BAND -> drawCurve(canvas, bounds, palette.accentArgb, top = true)
            BusinessCardLayoutBlueprint.AccentShape.BOTTOM_BAND -> drawCurve(canvas, bounds, palette.accentArgb, top = false)
            BusinessCardLayoutBlueprint.AccentShape.LEFT_BAND -> drawLeaf(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, leftSide = true)
            BusinessCardLayoutBlueprint.AccentShape.RIGHT_BAND -> drawLeaf(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, leftSide = false)
            BusinessCardLayoutBlueprint.AccentShape.RADIAL_GLOW -> drawSeed(canvas, bounds, palette.accentArgb)
            BusinessCardLayoutBlueprint.AccentShape.FULL_BLEED -> drawTwineBorder(canvas, bounds, palette.accentSecondaryArgb)
            else -> drawTwineBorder(canvas, bounds, palette.accentArgb)
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

    private fun drawGrain(canvas: Canvas, bounds: RectF, muted: Int, seed: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (muted and 0x00FFFFFF) or 0x14000000
            style = Paint.Style.FILL
        }
        var s = (seed * 2654435761L) and 0xFFFFFFFFL
        if (s == 0L) s = 1L
        val dotCount = 220
        for (i in 0 until dotCount) {
            s = (s * 6364136223846793005L + 1442695040888963407L) and 0x7FFFFFFFL
            val xRand = (s and 0xFFFF).toInt() / 65535f
            val yRand = ((s shr 16) and 0xFFFF).toInt() / 65535f
            val r = bounds.width() * 0.0009f
            canvas.drawCircle(bounds.left + xRand * bounds.width(), bounds.top + yRand * bounds.height(), r, paint)
        }
    }

    private fun drawCurve(canvas: Canvas, bounds: RectF, accent: Int, top: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.005f
        }
        val path = Path()
        if (top) {
            path.moveTo(bounds.left, bounds.top + bounds.height() * 0.18f)
            path.quadTo(bounds.centerX(), bounds.top - bounds.height() * 0.05f, bounds.right, bounds.top + bounds.height() * 0.20f)
        } else {
            path.moveTo(bounds.left, bounds.bottom - bounds.height() * 0.18f)
            path.quadTo(bounds.centerX(), bounds.bottom + bounds.height() * 0.10f, bounds.right, bounds.bottom - bounds.height() * 0.20f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawLeaf(canvas: Canvas, bounds: RectF, fill: Int, vein: Int, leftSide: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (fill and 0x00FFFFFF) or 0x55000000; style = Paint.Style.FILL }
        val path = Path()
        if (leftSide) {
            path.moveTo(bounds.left, bounds.top + bounds.height() * 0.35f)
            path.quadTo(bounds.left + bounds.width() * 0.30f, bounds.top + bounds.height() * 0.05f, bounds.left + bounds.width() * 0.55f, bounds.top + bounds.height() * 0.40f)
            path.quadTo(bounds.left + bounds.width() * 0.30f, bounds.top + bounds.height() * 0.95f, bounds.left, bounds.top + bounds.height() * 0.65f)
            path.close()
        } else {
            path.moveTo(bounds.right, bounds.top + bounds.height() * 0.35f)
            path.quadTo(bounds.right - bounds.width() * 0.30f, bounds.top + bounds.height() * 0.05f, bounds.right - bounds.width() * 0.55f, bounds.top + bounds.height() * 0.40f)
            path.quadTo(bounds.right - bounds.width() * 0.30f, bounds.top + bounds.height() * 0.95f, bounds.right, bounds.top + bounds.height() * 0.65f)
            path.close()
        }
        canvas.drawPath(path, paint)
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = vein; style = Paint.Style.STROKE; strokeWidth = bounds.width() * 0.0018f }
        if (leftSide) canvas.drawLine(bounds.left, bounds.top + bounds.height() * 0.50f, bounds.left + bounds.width() * 0.45f, bounds.top + bounds.height() * 0.50f, veinPaint)
        else canvas.drawLine(bounds.right, bounds.top + bounds.height() * 0.50f, bounds.right - bounds.width() * 0.45f, bounds.top + bounds.height() * 0.50f, veinPaint)
    }

    private fun drawSeed(canvas: Canvas, bounds: RectF, accent: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = (accent and 0x00FFFFFF) or 0x33000000; style = Paint.Style.FILL }
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        for (i in 0 until 12) {
            val angle = (i * Math.PI / 6.0).toFloat()
            val r = bounds.width() * 0.16f
            val px = cx + r * cos(angle)
            val py = cy + r * sin(angle)
            canvas.drawCircle(px, py, bounds.width() * 0.012f, paint)
        }
    }

    private fun drawTwineBorder(canvas: Canvas, bounds: RectF, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = bounds.width() * 0.0028f
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(bounds.width() * 0.012f, bounds.width() * 0.014f), 0f)
        }
        val inset = bounds.width() * 0.035f
        canvas.drawRoundRect(
            RectF(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset),
            bounds.width() * 0.018f, bounds.width() * 0.018f, paint,
        )
    }
}
