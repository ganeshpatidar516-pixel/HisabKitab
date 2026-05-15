package com.ganesh.hisabkitabpro.domain.businesscard.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardLayoutBlueprint
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardTypography
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier
import com.ganesh.hisabkitabpro.domain.businesscard.layout.CardLayoutGeometry
import com.ganesh.hisabkitabpro.domain.businesscard.layout.LayoutRect

/**
 * Shared low-level helpers for category painters. Centralises the recurring chores —
 * typeface mapping, text fitting, accent geometry, soft drop-shadows, logo placement —
 * so each painter file can focus on its aesthetic intent.
 */
internal object PainterToolkit {

    fun typeface(token: BusinessCardTypography.FontFamilyToken, weight: BusinessCardTypography.FontWeightToken): Typeface {
        val baseFamily = when (token) {
            BusinessCardTypography.FontFamilyToken.SERIF -> Typeface.SERIF
            BusinessCardTypography.FontFamilyToken.SANS_SERIF -> Typeface.SANS_SERIF
            BusinessCardTypography.FontFamilyToken.MONOSPACE -> Typeface.MONOSPACE
        }
        val style = when (weight) {
            BusinessCardTypography.FontWeightToken.THIN,
            BusinessCardTypography.FontWeightToken.LIGHT,
            BusinessCardTypography.FontWeightToken.REGULAR,
            BusinessCardTypography.FontWeightToken.MEDIUM -> Typeface.NORMAL
            BusinessCardTypography.FontWeightToken.SEMI_BOLD,
            BusinessCardTypography.FontWeightToken.BOLD,
            BusinessCardTypography.FontWeightToken.BLACK -> Typeface.BOLD
        }
        return Typeface.create(baseFamily, style)
    }

    /** Computes the largest font size (within the given range) at which [text] still fits in [maxWidth]. */
    fun fitFontSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        startSize: Float,
        minSize: Float,
        stepDownFactor: Float = 0.92f,
    ): Float {
        if (text.isBlank() || maxWidth <= 0f) return startSize
        var size = startSize
        paint.textSize = size
        while (paint.measureText(text) > maxWidth && size > minSize) {
            size *= stepDownFactor
            paint.textSize = size
        }
        return size
    }

    /** Wraps [text] into lines that each fit within [maxWidth] using the current paint. */
    fun wrapText(paint: Paint, text: String, maxWidth: Float, maxLines: Int = Int.MAX_VALUE): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(' ')
        val lines = ArrayList<String>(maxLines)
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current.clear()
                current.append(candidate)
            } else {
                if (current.isNotEmpty()) {
                    lines += current.toString()
                    if (lines.size >= maxLines) return lines
                    current = StringBuilder()
                }
                if (paint.measureText(word) <= maxWidth) {
                    current.append(word)
                } else {
                    var truncated = word
                    while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
                        truncated = truncated.dropLast(1)
                    }
                    lines += if (truncated.isEmpty()) "…" else "$truncated…"
                    if (lines.size >= maxLines) return lines
                }
            }
        }
        if (current.isNotEmpty()) lines += current.toString()
        return if (lines.size <= maxLines) lines else lines.take(maxLines)
    }

    /** Paints [text] left-aligned starting at (x, y) and returns the y advanced by line height. */
    fun drawTextLine(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        lineHeight: Float,
        alignment: BusinessCardLayoutBlueprint.TextAlignment,
        rightX: Float,
        centerX: Float,
    ): Float {
        if (text.isBlank()) return y
        when (alignment) {
            BusinessCardLayoutBlueprint.TextAlignment.LEAD_LEFT -> {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(text, x, y, paint)
            }
            BusinessCardLayoutBlueprint.TextAlignment.LEAD_RIGHT -> {
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(text, rightX, y, paint)
            }
            BusinessCardLayoutBlueprint.TextAlignment.CENTER -> {
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(text, centerX, y, paint)
            }
        }
        return y + lineHeight
    }

    /**
     * Renders [logo] into the slot at [target] honouring its native aspect classification.
     * For circular logos a circular mask is applied so the brand mark sits in a perfect medallion.
     */
    fun drawLogo(canvas: Canvas, paint: Paint, logo: Bitmap, target: RectF, shape: LogoAspectClassifier.LogoShape) {
        val srcRatio = logo.width.toFloat() / logo.height.toFloat()
        val dstRatio = target.width() / target.height()
        val drawRect = RectF(target)
        if (srcRatio > dstRatio) {
            val newHeight = target.width() / srcRatio
            val pad = (target.height() - newHeight) / 2f
            drawRect.top = target.top + pad
            drawRect.bottom = target.bottom - pad
        } else {
            val newWidth = target.height() * srcRatio
            val pad = (target.width() - newWidth) / 2f
            drawRect.left = target.left + pad
            drawRect.right = target.right - pad
        }
        if (shape == LogoAspectClassifier.LogoShape.CIRCLE) {
            val saved = canvas.saveLayer(target, null)
            canvas.drawOval(target, paint.apply { isAntiAlias = true; color = -0x1; style = Paint.Style.FILL })
            val restorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            }
            canvas.drawBitmap(logo, null, drawRect, restorePaint)
            restorePaint.xfermode = null
            canvas.restoreToCount(saved)
        } else {
            val draw = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawBitmap(logo, null, drawRect, draw)
        }
    }

    fun drawQr(canvas: Canvas, qr: Bitmap, target: RectF, surfacePaint: Paint? = null) {
        if (surfacePaint != null) {
            canvas.drawRoundRect(target, target.width() * 0.06f, target.width() * 0.06f, surfacePaint)
        }
        val pad = target.width() * 0.06f
        val inner = RectF(
            target.left + pad,
            target.top + pad,
            target.right - pad,
            target.bottom - pad,
        )
        val draw = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(qr, null, inner, draw)
    }

    /** Soft, premium drop-shadow used by every painter when the surface differs from the background. */
    fun softShadow(paint: Paint, radius: Float, dy: Float, color: Int) {
        paint.setShadowLayer(radius, 0f, dy, color)
    }

    fun makeLinearGradient(rect: RectF, fromColor: Int, toColor: Int, vertical: Boolean = false): Shader {
        return if (vertical) {
            LinearGradient(rect.left, rect.top, rect.left, rect.bottom, fromColor, toColor, Shader.TileMode.CLAMP)
        } else {
            LinearGradient(rect.left, rect.top, rect.right, rect.bottom, fromColor, toColor, Shader.TileMode.CLAMP)
        }
    }

    fun makeRadialGlow(centerX: Float, centerY: Float, radius: Float, color: Int): Shader {
        return RadialGradient(centerX, centerY, radius, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
    }

    /** Slot helpers — geometry lives in [CardLayoutGeometry]; this stays for [RectF] call sites. */
    fun logoSlot(bounds: RectF, anchor: BusinessCardLayoutBlueprint.LogoAnchor, padding: Float, sideLength: Float): RectF =
        CardLayoutGeometry.logoSlot(bounds.toLayoutRect(), anchor, padding, sideLength).toRectF()

    fun qrSlot(bounds: RectF, placement: BusinessCardLayoutBlueprint.QrPlacement, padding: Float, sideLength: Float): RectF =
        CardLayoutGeometry.qrSlot(bounds.toLayoutRect(), placement, padding, sideLength).toRectF()

    private fun RectF.toLayoutRect() = LayoutRect(left, top, right, bottom)

    private fun LayoutRect.toRectF() = RectF(left, top, right, bottom)

    /** Suppresses the unused warning while still keeping the shader factory available to renderers. */
    fun ignored(matrix: Matrix?, path: Path?) { matrix?.reset(); path?.reset() }
}
