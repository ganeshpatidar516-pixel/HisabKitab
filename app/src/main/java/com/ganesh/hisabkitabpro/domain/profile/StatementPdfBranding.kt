package com.ganesh.hisabkitabpro.domain.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile

/**
 * Shared header strip for **statement-style** Canvas PDFs (customer ledger, supplier statement).
 * Draws optional merchant logo (left) and payment QR (top-right); returns the X coordinate
 * where the business title should begin.
 */
object StatementPdfBranding {

    private const val PAGE_WIDTH = 595f
    private const val LOGO_SIZE = 56f
    private const val QR_SIZE = 56

    /**
     * @param titleBaselineY baseline Y for the first main title line (e.g. business name)
     * @param margin left/right margin (ledger uses 50f, supplier flows often use 32f)
     */
    fun titleStartXAfterBranding(
        context: Context,
        canvas: Canvas,
        profile: BusinessProfile?,
        margin: Float,
        titleBaselineY: Float,
    ): Float {
        var titleLeft = margin
        val app = context.applicationContext
        profile?.logoUrl?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(app, it) }?.let { logo ->
            val top = titleBaselineY - 38f
            val slot = RectF(margin, top, margin + LOGO_SIZE, top + LOGO_SIZE)
            LogoRenderFit.drawWithinRect(canvas, logo, slot)
            if (!logo.isRecycled) logo.recycle()
            titleLeft = slot.right + 8f
        }
        profile?.qrImagePath?.let { ProfileBitmapLoader.loadBitmapForPdfEmbed(app, it) }?.let { qrRaw ->
            val scaled = Bitmap.createScaledBitmap(qrRaw, QR_SIZE, QR_SIZE, true)
            canvas.drawBitmap(scaled, PAGE_WIDTH - margin - QR_SIZE, 18f, null)
            if (scaled != qrRaw && !scaled.isRecycled) scaled.recycle()
            if (!qrRaw.isRecycled) qrRaw.recycle()
        }
        return titleLeft
    }
}
