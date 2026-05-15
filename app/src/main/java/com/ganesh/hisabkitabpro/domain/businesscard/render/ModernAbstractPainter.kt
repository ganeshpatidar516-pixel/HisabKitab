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
 * Modern Abstract — procedurally generated geometric fields. Every shape is computed
 * from the variation seed using a tiny linear congruential generator so the result is
 * deterministic and unique per card with no external SVG assets.
 */
internal object ModernAbstractPainter : BusinessCardPainter {

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

        val rng = MiniLCG(variation.seed.toLong())
        when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.HAIRLINE_GRID -> drawConcentricArcs(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, rng)
            BusinessCardLayoutBlueprint.AccentShape.RADIAL_GLOW -> drawCirclesField(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, rng)
            BusinessCardLayoutBlueprint.AccentShape.FULL_BLEED -> drawTriangulation(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, rng)
            else -> drawTrapezoidStrips(canvas, bounds, palette.accentArgb, palette.accentSecondaryArgb, blueprint, rng)
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

    private fun drawConcentricArcs(canvas: Canvas, bounds: RectF, c1: Int, c2: Int, rng: MiniLCG) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }
        val cx = bounds.right - bounds.width() * 0.08f
        val cy = bounds.bottom - bounds.height() * 0.10f
        val maxRadius = bounds.width() * 0.65f
        var radius = bounds.width() * 0.10f
        var i = 0
        while (radius < maxRadius) {
            paint.color = if (i % 2 == 0) c1 else c2
            paint.strokeWidth = bounds.width() * (0.0025f + rng.unit() * 0.003f)
            canvas.drawCircle(cx, cy, radius, paint)
            radius += bounds.width() * (0.024f + rng.unit() * 0.014f)
            i++
        }
    }

    private fun drawCirclesField(canvas: Canvas, bounds: RectF, c1: Int, c2: Int, rng: MiniLCG) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val count = 9
        for (i in 0 until count) {
            val cx = bounds.left + rng.unit() * bounds.width()
            val cy = bounds.top + rng.unit() * bounds.height()
            val r = bounds.width() * (0.03f + rng.unit() * 0.07f)
            paint.color = (if (i % 2 == 0) c1 else c2).withAlpha(60 + (rng.unit() * 80f).toInt())
            canvas.drawCircle(cx, cy, r, paint)
        }
    }

    private fun drawTriangulation(canvas: Canvas, bounds: RectF, c1: Int, c2: Int, rng: MiniLCG) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val rows = 6
        val cols = 10
        val cellW = bounds.width() / cols
        val cellH = bounds.height() / rows
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val left = bounds.left + c * cellW
                val top = bounds.top + r * cellH
                val pick = (r + c + (rng.next() and 1).toInt()) % 4
                val path = Path()
                when (pick) {
                    0 -> { path.moveTo(left, top); path.lineTo(left + cellW, top); path.lineTo(left, top + cellH); path.close() }
                    1 -> { path.moveTo(left + cellW, top); path.lineTo(left + cellW, top + cellH); path.lineTo(left, top); path.close() }
                    2 -> { path.moveTo(left, top + cellH); path.lineTo(left + cellW, top + cellH); path.lineTo(left + cellW, top); path.close() }
                    else -> { path.moveTo(left, top); path.lineTo(left + cellW, top + cellH); path.lineTo(left, top + cellH); path.close() }
                }
                paint.color = (if ((r + c) % 2 == 0) c1 else c2).withAlpha(28 + (rng.unit() * 28f).toInt())
                canvas.drawPath(path, paint)
            }
        }
    }

    private fun drawTrapezoidStrips(canvas: Canvas, bounds: RectF, c1: Int, c2: Int, blueprint: BusinessCardLayoutBlueprint, rng: MiniLCG) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val angleSeed = (rng.unit() * 60f) - 30f
        val baseY = when (blueprint.accentShape) {
            BusinessCardLayoutBlueprint.AccentShape.TOP_BAND -> bounds.top
            BusinessCardLayoutBlueprint.AccentShape.BOTTOM_BAND -> bounds.bottom - bounds.height() * 0.4f
            else -> bounds.top + bounds.height() * 0.45f
        }
        val sliceCount = 4
        val sliceW = bounds.width() / sliceCount.toFloat()
        for (i in 0 until sliceCount) {
            val left = bounds.left + sliceW * i
            val right = left + sliceW
            val top = baseY + (sin((i + 1) * angleSeed) * bounds.height() * 0.05f).toFloat()
            val bottom = baseY + bounds.height() * (0.30f + cos((i + 2) * angleSeed) * 0.08f).toFloat()
            val path = Path().apply {
                moveTo(left, top)
                lineTo(right, top + bounds.height() * 0.05f * (i + 1) / sliceCount)
                lineTo(right, bottom)
                lineTo(left, bottom - bounds.height() * 0.04f)
                close()
            }
            paint.color = if (i % 2 == 0) c1 else c2
            paint.alpha = 80 - (i * 8)
            canvas.drawPath(path, paint)
        }
    }

    private fun Int.withAlpha(alpha: Int): Int {
        val safe = alpha.coerceIn(0, 255)
        return (this and 0x00FFFFFF) or (safe shl 24)
    }

    /** Tiny seeded LCG (Numerical Recipes parameters). Pure, deterministic, no JDK Random object allocations. */
    private class MiniLCG(seed: Long) {
        private var state: Long = if (seed == 0L) 0xDEADBEEFL else seed
        fun next(): Long {
            state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
            return state
        }
        fun unit(): Float = (next().toInt() and 0x7FFFFFFF).toFloat() / Int.MAX_VALUE.toFloat()
    }
}
