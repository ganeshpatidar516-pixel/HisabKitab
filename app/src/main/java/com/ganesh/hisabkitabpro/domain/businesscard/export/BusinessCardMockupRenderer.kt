package com.ganesh.hisabkitabpro.domain.businesscard.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a single card on top of a softly-lit neutral background and a subtle floor
 * shadow — the kind of "product mockup" composition that performs well on WhatsApp
 * status, Instagram and LinkedIn shares.
 */
object BusinessCardMockupRenderer {

    private const val MOCKUP_WIDTH_PX = 1440
    private const val MOCKUP_HEIGHT_PX = 1440

    suspend fun renderMockup(
        context: Context,
        variation: BusinessCardVariation,
        profile: BusinessCardProfile,
        logoPath: String?,
        qrBitmap: Bitmap?,
    ): Uri? {
        var cardBmp: Bitmap? = null
        var mockup: Bitmap? = null
        // Mockup renders a single card — we decode the logo once here so the renderer
        // doesn't have to decode-and-recycle inline (cheap), and we guarantee a single
        // ownership path regardless of which branch of the try/finally runs.
        var sharedLogo: Bitmap? = null
        val cardWidthPx = (MOCKUP_WIDTH_PX * 0.74f).toInt()
        try {
            sharedLogo = BusinessCardBitmapRenderer.decodeSharedLogo(context, logoPath, cardWidthPx)
            val cardRender = BusinessCardBitmapRenderer.render(
                context = context.applicationContext,
                variation = variation,
                profile = profile,
                logoPath = logoPath,
                qrBitmap = qrBitmap,
                widthPx = cardWidthPx,
                sharedLogo = sharedLogo,
            )
            cardBmp = cardRender.bitmap

            mockup = Bitmap.createBitmap(MOCKUP_WIDTH_PX, MOCKUP_HEIGHT_PX, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(mockup!!)
            paintBackdrop(canvas, MOCKUP_WIDTH_PX.toFloat(), MOCKUP_HEIGHT_PX.toFloat())

            val cardW = cardRender.widthPx.toFloat()
            val cardH = cardRender.heightPx.toFloat()
            val left = (MOCKUP_WIDTH_PX - cardW) / 2f
            val top = (MOCKUP_HEIGHT_PX - cardH) / 2f
            val target = RectF(left, top, left + cardW, top + cardH)

            paintFloorShadow(canvas, target)
            canvas.drawBitmap(cardBmp, null, target, Paint(Paint.FILTER_BITMAP_FLAG))

            val outDir = File(context.cacheDir, "business_cards").apply { mkdirs() }
            val outFile = File(outDir, "mockup_${variation.id}_${System.currentTimeMillis()}.png")
            FileOutputStream(outFile).use { fos -> mockup!!.compress(Bitmap.CompressFormat.PNG, 95, fos) }
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", outFile)
        } catch (t: Throwable) {
            android.util.Log.e(
                "BusinessCardEngine",
                "Mockup render failed: variation=${variation.id}, category=${variation.category}",
                t
            )
            return null
        } finally {
            cardBmp?.let { if (!it.isRecycled) it.recycle() }
            mockup?.let { if (!it.isRecycled) it.recycle() }
            sharedLogo?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    private fun paintBackdrop(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, 0f, height, intArrayOf(0xFFF6F7FB.toInt(), 0xFFE6E9F2.toInt(), 0xFFD3D8E6.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width, height, paint)
        // Subtle vignette so the eye is led to the card.
        val vignette = Paint().apply {
            shader = android.graphics.RadialGradient(width / 2f, height / 2f, width * 0.7f, 0x00000000, 0x33000000, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width, height, vignette)
    }

    private fun paintFloorShadow(canvas: Canvas, card: RectF) {
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 0, 0, 0)
            maskFilter = BlurMaskFilter(card.width() * 0.06f, BlurMaskFilter.Blur.NORMAL)
        }
        val sRect = RectF(
            card.left + card.width() * 0.04f,
            card.bottom - card.height() * 0.05f,
            card.right - card.width() * 0.04f,
            card.bottom + card.height() * 0.20f,
        )
        canvas.drawOval(sRect, shadow)
    }
}
