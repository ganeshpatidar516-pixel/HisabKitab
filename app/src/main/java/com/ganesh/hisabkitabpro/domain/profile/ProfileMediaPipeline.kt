package com.ganesh.hisabkitabpro.domain.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-end QR fitment layer. It normalizes camera/gallery input into a square,
 * print-stable PNG using:
 * - luminance edge detection for tight QR cropping
 * - center-inside foreground scaling
 * - blurred, colour-matched background fill
 * - optional PhonePe-style circular logo badge overlay
 *
 * This is intentionally local-only and does not alter existing profile persistence.
 */
object ProfileMediaPipeline {

    private const val OUTPUT_SIZE = 1024
    private const val FOREGROUND_FRACTION = 0.86f

    fun processQrImage(
        context: Context,
        sourceUri: Uri,
        logoPath: String?,
        outputName: String = "business_qr_processed.png",
    ): File {
        val source = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        } ?: error("Unable to decode QR image")
        return processQrBitmap(context, source, logoPath, outputName)
    }

    fun processQrBitmap(
        context: Context,
        source: Bitmap,
        logoPath: String?,
        outputName: String = "business_qr_processed.png",
    ): File {
        val cropped = cropToQrEdges(source)
        val output = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val dominant = dominantColor(cropped)
        val blurred = scaledBlur(cropped, OUTPUT_SIZE)
        canvas.drawColor(dominant)
        canvas.drawBitmap(blurred, null, Rect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE), Paint(Paint.FILTER_BITMAP_FLAG))

        val side = (OUTPUT_SIZE * FOREGROUND_FRACTION).toInt()
        val inset = (OUTPUT_SIZE - side) / 2
        val dst = Rect(inset, inset, inset + side, inset + side)
        canvas.drawBitmap(cropped, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        decodeLogo(logoPath)?.let { logo ->
            drawLogoBadge(canvas, logo)
            if (logo !== cropped && !logo.isRecycled) logo.recycle()
        }

        val outFile = File(com.ganesh.hisabkitabpro.core.storage.AppStoragePaths.mediaQrDir(context), outputName)
        FileOutputStream(outFile).use { fos -> output.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        if (cropped !== source && !cropped.isRecycled) cropped.recycle()
        if (!blurred.isRecycled) blurred.recycle()
        if (!output.isRecycled) output.recycle()
        return outFile
    }

    private fun cropToQrEdges(source: Bitmap): Bitmap {
        val maxSide = max(source.width, source.height).coerceAtLeast(1)
        val sample = (maxSide / 900).coerceAtLeast(1)
        val small = if (sample > 1) Bitmap.createScaledBitmap(source, source.width / sample, source.height / sample, true) else source
        val threshold = estimateThreshold(small)
        var left = small.width
        var top = small.height
        var right = 0
        var bottom = 0

        val bg = Color.WHITE
        for (y in 0 until small.height) {
            for (x in 0 until small.width) {
                val pixel = small.getPixel(x, y)
                if (abs(luma(pixel) - luma(bg)) > threshold) {
                    left = min(left, x)
                    top = min(top, y)
                    right = max(right, x)
                    bottom = max(bottom, y)
                }
            }
        }

        if (right <= left || bottom <= top) return source
        val pad = ((right - left).coerceAtLeast(bottom - top) * 0.08f).toInt()
        val scaleBack = sample
        val srcLeft = ((left - pad).coerceAtLeast(0) * scaleBack).coerceAtMost(source.width - 1)
        val srcTop = ((top - pad).coerceAtLeast(0) * scaleBack).coerceAtMost(source.height - 1)
        val srcRight = ((right + pad).coerceAtMost(small.width - 1) * scaleBack).coerceAtLeast(srcLeft + 1).coerceAtMost(source.width)
        val srcBottom = ((bottom + pad).coerceAtMost(small.height - 1) * scaleBack).coerceAtLeast(srcTop + 1).coerceAtMost(source.height)
        if (small !== source && !small.isRecycled) small.recycle()
        return Bitmap.createBitmap(source, srcLeft, srcTop, srcRight - srcLeft, srcBottom - srcTop)
    }

    private fun estimateThreshold(bitmap: Bitmap): Int {
        var total = 0
        var count = 0
        val step = max(bitmap.width, bitmap.height).coerceAtLeast(1) / 64 + 1
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                total += luma(bitmap.getPixel(x, y))
                count++
            }
        }
        val avg = if (count == 0) 245 else total / count
        return if (avg > 200) 42 else 28
    }

    private fun scaledBlur(source: Bitmap, size: Int): Bitmap {
        val tiny = Bitmap.createScaledBitmap(source, 48, 48, true)
        val pixels = IntArray(tiny.width * tiny.height)
        tiny.getPixels(pixels, 0, tiny.width, 0, 0, tiny.width, tiny.height)
        repeat(3) { boxBlur(pixels, tiny.width, tiny.height) }
        tiny.setPixels(pixels, 0, tiny.width, 0, 0, tiny.width, tiny.height)
        val out = Bitmap.createScaledBitmap(tiny, size, size, true)
        tiny.recycle()
        return out
    }

    private fun boxBlur(pixels: IntArray, width: Int, height: Int) {
        val copy = pixels.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0
                for (yy in -1..1) {
                    for (xx in -1..1) {
                        val c = copy[(y + yy) * width + (x + xx)]
                        r += Color.red(c)
                        g += Color.green(c)
                        b += Color.blue(c)
                    }
                }
                pixels[y * width + x] = Color.rgb(r / 9, g / 9, b / 9)
            }
        }
    }

    private fun drawLogoBadge(canvas: Canvas, logo: Bitmap) {
        val center = OUTPUT_SIZE / 2f
        val badgeRadius = OUTPUT_SIZE * 0.105f
        val logoRadius = OUTPUT_SIZE * 0.075f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawCircle(center, center, badgeRadius, paint)
        paint.color = Color.rgb(245, 246, 248)
        canvas.drawCircle(center, center, badgeRadius * 0.88f, paint)

        val slot = RectF(center - logoRadius, center - logoRadius, center + logoRadius, center + logoRadius)
        val saved = canvas.saveLayer(slot, null)
        canvas.drawOval(slot, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
        val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        canvas.drawBitmap(logo, null, slot, logoPaint)
        logoPaint.xfermode = null
        canvas.restoreToCount(saved)
    }

    private fun decodeLogo(path: String?): Bitmap? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            val maxDim = max(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDim / sample > 512) sample *= 2
            BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }

    private fun dominantColor(bitmap: Bitmap): Int {
        val step = max(bitmap.width, bitmap.height).coerceAtLeast(1) / 32 + 1
        var r = 0
        var g = 0
        var b = 0
        var count = 0
        for (y in 0 until bitmap.height step step) {
            for (x in 0 until bitmap.width step step) {
                val c = bitmap.getPixel(x, y)
                r += Color.red(c)
                g += Color.green(c)
                b += Color.blue(c)
                count++
            }
        }
        if (count == 0) return Color.WHITE
        return Color.rgb(r / count, g / count, b / count)
    }

    private fun luma(color: Int): Int =
        ((Color.red(color) * 299) + (Color.green(color) * 587) + (Color.blue(color) * 114)) / 1000
}
