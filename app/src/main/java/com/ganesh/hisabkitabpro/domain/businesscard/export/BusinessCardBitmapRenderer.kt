package com.ganesh.hisabkitabpro.domain.businesscard.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.GoldenRatioGrid
import com.ganesh.hisabkitabpro.domain.businesscard.LogoAspectClassifier
import com.ganesh.hisabkitabpro.domain.businesscard.render.BusinessCardPainterRegistry
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader

/**
 * Renders one [BusinessCardVariation] off-screen at the requested density.
 *
 * Defaults to a print-ready 88×54 mm card at 320 DPI which yields ~1109×680 px and
 * comfortably exceeds the 300 DPI threshold required by professional print houses.
 *
 * ## Logo lifecycle (CRITICAL for memory stability)
 *
 * The renderer supports **two ownership models** so the export pipeline does not
 * leak 50× decoded logo bitmaps:
 *
 * 1. **Caller-owned (shared logo)** — pass [sharedLogo] (already decoded) and the
 *    renderer will use it without recycling. This is the path used by
 *    [BusinessCardPdfExporter.exportAll] which decodes the logo exactly once and
 *    reuses it for all 50 pages.
 * 2. **Renderer-owned (one-shot)** — pass `sharedLogo = null` together with
 *    [logoPath]; the renderer decodes the logo internally and **recycles it before
 *    returning**. This is the safe default for single renders.
 *
 * Either way, the returned [Render.bitmap] is **owned by the caller** and must be
 * recycled after use.
 */
object BusinessCardBitmapRenderer {

    /** Standard ISO 7810 ID-1 card width in millimetres. */
    private const val CARD_WIDTH_MM = 88f
    private const val DEFAULT_DPI = 320f
    private const val MM_PER_INCH = 25.4f

    data class Render(
        val bitmap: Bitmap,
        val widthPx: Int,
        val heightPx: Int,
    )

    /**
     * Renders one card. If [sharedLogo] is supplied it is reused as-is (and never
     * recycled by this method); otherwise the logo is decoded from [logoPath] and
     * recycled internally before this method returns — preventing the historical
     * 50× logo-decode leak in [BusinessCardPdfExporter.exportAll].
     */
    fun render(
        context: Context,
        variation: BusinessCardVariation,
        profile: BusinessCardProfile,
        logoPath: String?,
        qrBitmap: Bitmap?,
        widthPx: Int = printWidthPx(),
        sharedLogo: Bitmap? = null,
    ): Render {
        require(widthPx > 0) { "widthPx must be positive" }
        val heightPx = (widthPx / GoldenRatioGrid.PHI).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bounds = RectF(0f, 0f, widthPx.toFloat(), heightPx.toFloat())

        val callerOwnsLogo = sharedLogo != null && !sharedLogo.isRecycled
        val logo: Bitmap? = if (callerOwnsLogo) {
            sharedLogo
        } else {
            ProfileBitmapLoader.loadBitmapMaxSide(
                context.applicationContext,
                logoPath,
                widthPx.coerceAtLeast(64),
            )
        }
        val logoShape = if (logo != null) {
            LogoAspectClassifier.classify(logo.width, logo.height, prefersCircular = false)
        } else LogoAspectClassifier.LogoShape.SQUARE

        val painter = BusinessCardPainterRegistry.painterFor(variation.category)
        try {
            painter.paint(canvas, bounds, variation, profile, logo, qrBitmap, logoShape)
        } catch (t: Throwable) {
            Log.e(
                "BusinessCardEngine",
                "Bitmap render failed: variation=${variation.id}, category=${variation.category}",
                t
            )
            // Ensure the exporter can continue for other templates even if one template breaks.
            canvas.drawColor(0xFFECEFF3.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFB00020.toInt()
                textSize = (widthPx * 0.04f).coerceIn(16f, 34f)
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isSubpixelText = true
            }
            canvas.drawText("Template error", bounds.centerX(), bounds.centerY(), paint)
        } finally {
            // Only recycle the logo we ourselves decoded — never touch caller-supplied bitmaps.
            if (!callerOwnsLogo && logo != null && !logo.isRecycled) {
                logo.recycle()
            }
        }

        return Render(bitmap, widthPx, heightPx)
    }

    /**
     * Decodes the business logo once at the print-grade resolution used by [render].
     * The caller takes ownership and must recycle the returned bitmap. Returns null
     * when [logoPath] is blank or decoding fails — callers should fall back to a
     * logo-less render rather than crash.
     */
    fun decodeSharedLogo(context: Context, logoPath: String?, widthPx: Int = printWidthPx()): Bitmap? {
        if (logoPath.isNullOrBlank()) return null
        return ProfileBitmapLoader.loadBitmapMaxSide(
            context.applicationContext,
            logoPath,
            widthPx.coerceAtLeast(64),
        )
    }

    fun printWidthPx(dpi: Float = DEFAULT_DPI): Int {
        return ((CARD_WIDTH_MM / MM_PER_INCH) * dpi).toInt()
    }
}
