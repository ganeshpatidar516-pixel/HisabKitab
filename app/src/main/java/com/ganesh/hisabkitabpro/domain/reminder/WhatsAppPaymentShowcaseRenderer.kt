package com.ganesh.hisabkitabpro.domain.reminder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.ProfileBitmapLoader
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Renders a **high-resolution payment showcase card** for WhatsApp: QR-first hierarchy, generous
 * quiet zone, and light background so the code stays scannable after in-app compression.
 *
 * On failure returns `null` so callers can fall back to the raw static QR file.
 */
object WhatsAppPaymentShowcaseRenderer {

    private const val OUT_WIDTH = 1080
    private const val H_PAD = 52f
    private const val TOP_STRIP = 10f
    /** Outer padding around the QR module inside the white well (WhatsApp-safe quiet zone). */
    private const val QR_FRAME_PAD = 42f
    /** Target: QR module occupies ~38% of inner content width (30–45% spec band). */
    private const val QR_MODULE_FRAC_OF_INNER = 0.38f
    private const val MIN_CANVAS_H = 1580
    private const val MAX_CANVAS_H = 2200

    private const val SHOWCASE_CACHE_PREFIX = "whatsapp_payment_showcase_"
    private const val SHOWCASE_CACHE_SUFFIX = ".png"
    /** TTL-based cleanup prevents races between concurrent reminder renders. */
    private const val SHOWCASE_CACHE_TTL_MS: Long = 2L * 60L * 1000L // 2 minutes

    /** Removes prior showcase PNGs so cache does not grow without bound. */
    fun deleteOldShowcaseCacheFiles(cacheDir: File) {
        val now = System.currentTimeMillis()
        cacheDir.listFiles()?.forEach { f ->
            if (f.isFile &&
                f.name.startsWith(SHOWCASE_CACHE_PREFIX) &&
                f.name.endsWith(SHOWCASE_CACHE_SUFFIX) &&
                now - f.lastModified() > SHOWCASE_CACHE_TTL_MS
            ) {
                runCatching { f.delete() }
            }
        }
    }

    /**
     * Writes a PNG under [context.cacheDir] and returns the file, or `null` if decoding/rendering fails.
     * Labels follow the language saved in app settings ([AppLocaleManager]).
     */
    fun renderToCacheFileOrNull(
        context: Context,
        profile: BusinessProfile?,
        customerName: String,
        amountPaise: Long,
        qrImageFile: File,
    ): File? {
        val app = context.applicationContext
        val src = runCatching {
            BitmapFactory.decodeFile(qrImageFile.absolutePath)?.takeIf { !it.isRecycled && it.width > 8 && it.height > 8 }
        }.getOrNull() ?: return null

        deleteOldShowcaseCacheFiles(app.cacheDir)

        val lc = AppLocaleManager.wrapContext(app)
        val amountLocale = lc.resources.configuration.locales.get(0) ?: Locale.getDefault()
        val amountDisplay = NumberFormat.getCurrencyInstance(amountLocale).format(amountPaise / 100.0)
        val w = OUT_WIDTH
        val innerW = w - 2 * H_PAD
        val qrModulePx = (innerW * QR_MODULE_FRAC_OF_INNER).roundToInt().coerceIn(380, 500)
        val wellOuterW = qrModulePx + 2 * QR_FRAME_PAD
        val wellLeft = (w - wellOuterW) / 2f

        val business = profile?.businessName?.trim()?.ifBlank { null }
            ?: lc.getString(R.string.whatsapp_showcase_business_default)
        val upi = profile?.upiId?.trim().orEmpty()

        val bg = 0xFFF4F6FB.toInt()
        val ink = 0xFF0F172A.toInt()
        val muted = 0xFF475569.toInt()
        val accent = 0xFF0D9488.toInt()
        val amountColor = 0xFF14532D.toInt()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 46f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isSubpixelText = true
        }
        val nameLayout = StaticLayout.Builder.obtain(business, 0, business.length, titlePaint, innerW.roundToInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .setLineSpacing(0f, 1.05f)
            .build()

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 32f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val subLine = lc.getString(R.string.whatsapp_showcase_for_customer, customerName.trim().ifBlank { "—" })
        val subLayout = StaticLayout.Builder.obtain(subLine, 0, subLine.length, subPaint, innerW.roundToInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()

        val amountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = amountColor
            textSize = 52f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val amountLayout = StaticLayout.Builder.obtain(amountDisplay, 0, amountDisplay.length, amountPaint, innerW.roundToInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(1)
            .build()

        val scanPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 26f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val scanLine = lc.getString(R.string.whatsapp_showcase_scan_here)
        val scanLayout = StaticLayout.Builder.obtain(scanLine, 0, scanLine.length, scanPaint, innerW.roundToInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(1)
            .build()

        val footPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 28f
        }
        val payTo = if (upi.isNotEmpty()) {
            lc.getString(R.string.whatsapp_showcase_pay_to, upi)
        } else {
            lc.getString(R.string.whatsapp_showcase_pay_to_placeholder)
        }
        val footLayout = StaticLayout.Builder.obtain(payTo, 0, payTo.length, footPaint, innerW.roundToInt())
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setMaxLines(2)
            .build()

        var y = H_PAD + TOP_STRIP + 20f
        y += scanLayout.height + 36f

        val wellTop = y
        val wellOuterH = qrModulePx + 2 * QR_FRAME_PAD
        y += wellOuterH + 56f

        y += nameLayout.height + 28f
        y += subLayout.height + 20f
        y += amountLayout.height + 44f
        y += footLayout.height + H_PAD + 32f

        val h = y.roundToInt().coerceIn(MIN_CANVAS_H, MAX_CANVAS_H)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(bg)

        val strip = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        canvas.drawRect(0f, 0f, w.toFloat(), TOP_STRIP, strip)

        val logo = ProfileBitmapLoader.loadBitmapMaxSide(app, profile?.logoUrl, 160)
        logo?.let { lb ->
            val side = 56
            val lx = w - H_PAD - side
            val ly = H_PAD + TOP_STRIP
            val dst = RectF(lx, ly, lx + side, ly + side)
            val srcR = Rect(0, 0, lb.width, lb.height)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(lb, srcR, dst, paint)
            if (!lb.isRecycled) lb.recycle()
        }

        var drawY = H_PAD + TOP_STRIP + 20f
        canvas.save()
        canvas.translate(H_PAD, drawY)
        scanLayout.draw(canvas)
        canvas.restore()
        drawY += scanLayout.height + 36f

        val wellRect = RectF(wellLeft, drawY, wellLeft + wellOuterW, drawY + wellOuterH)
        val rx = 28f
        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22000000 }
        canvas.drawRoundRect(wellRect.left + 4f, wellRect.top + 10f, wellRect.right + 4f, wellRect.bottom + 10f, rx + 4f, rx + 4f, shadow)
        val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        canvas.drawRoundRect(wellRect, rx, rx, card)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = 0xFFE2E8F0.toInt()
        }
        canvas.drawRoundRect(wellRect, rx, rx, stroke)

        val scaledQr = Bitmap.createScaledBitmap(src, qrModulePx, qrModulePx, false)
        val innerLeft = wellRect.left + QR_FRAME_PAD
        val innerTop = wellRect.top + QR_FRAME_PAD
        val dstQr = RectF(innerLeft, innerTop, innerLeft + qrModulePx, innerTop + qrModulePx)
        canvas.drawBitmap(scaledQr, null, dstQr, Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = false })
        if (!scaledQr.isRecycled) scaledQr.recycle()
        if (!src.isRecycled) src.recycle()

        drawY += wellOuterH + 56f

        canvas.save()
        canvas.translate(H_PAD, drawY)
        nameLayout.draw(canvas)
        canvas.restore()
        drawY += nameLayout.height + 28f

        canvas.save()
        canvas.translate(H_PAD, drawY)
        subLayout.draw(canvas)
        canvas.restore()
        drawY += subLayout.height + 20f

        canvas.save()
        canvas.translate(H_PAD, drawY)
        amountLayout.draw(canvas)
        canvas.restore()
        drawY += amountLayout.height + 44f

        canvas.save()
        canvas.translate(H_PAD, drawY)
        footLayout.draw(canvas)
        canvas.restore()

        val outFile = File(app.cacheDir, "${SHOWCASE_CACHE_PREFIX}${System.currentTimeMillis()}$SHOWCASE_CACHE_SUFFIX")
        return runCatching {
            FileOutputStream(outFile).use { fos ->
                if (!out.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    throw IllegalStateException("compress_failed")
                }
            }
            out.recycle()
            outFile
        }.getOrElse {
            if (!out.isRecycled) out.recycle()
            outFile.delete()
            null
        }
    }
}
