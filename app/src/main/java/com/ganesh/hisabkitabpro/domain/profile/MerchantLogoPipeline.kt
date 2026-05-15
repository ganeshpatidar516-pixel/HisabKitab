package com.ganesh.hisabkitabpro.domain.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Normalizes any merchant logo (PNG/JPEG/WEBP, any aspect) into a square, letterboxed,
 * print-safe PNG at [LOGO_MASTER_BOX] without stretching. Used by Business Profile save path.
 */
object MerchantLogoPipeline {

    /** Master logo canvas (px); letterboxed, transparent padding, down/up-scaled to fit. */
    const val LOGO_MASTER_BOX = 768

    /** Decode step: avoid loading huge originals into memory at full resolution. */
    private const val READ_MAX_SIDE = 2048

    /**
     * Reads [uri] (gallery/camera picker), normalizes, writes [outputFile] as PNG (overwrite).
     */
    fun processMerchantLogoFromUri(context: Context, uri: Uri, outputFile: File): File {
        val decoded = decodeSampledFromUri(context, uri, READ_MAX_SIDE)
            ?: error("Unable to read image")
        val normalized = renderCenterLetterbox(decoded, LOGO_MASTER_BOX)
        writeNormalizedPng(normalized, outputFile)
        return outputFile
    }

    /**
     * Re-encodes an on-disk logo (legacy raw copy) into the canonical square master, in place.
     * Safe for cold-start migration: writes via temp file then replaces.
     *
     * @return false if decode/write failed (caller may retry later).
     */
    fun normalizeExistingLogoFile(logoFile: File): Boolean = runCatching {
        if (!logoFile.isFile || logoFile.length() == 0L) return false
        val decoded = decodeSampledFromFile(logoFile, READ_MAX_SIDE) ?: return false
        val normalized = renderCenterLetterbox(decoded, LOGO_MASTER_BOX)
        val tmp = File(logoFile.parentFile, "${logoFile.name}.tmp")
        try {
            FileOutputStream(tmp).use { fos ->
                normalized.compress(Bitmap.CompressFormat.PNG, 92, fos)
            }
        } finally {
            if (!normalized.isRecycled) normalized.recycle()
        }
        if (!tmp.isFile || tmp.length() == 0L) {
            tmp.delete()
            return false
        }
        val deleted = !logoFile.exists() || logoFile.delete()
        if (!deleted) {
            tmp.delete()
            return false
        }
        if (!tmp.renameTo(logoFile)) {
            tmp.copyTo(logoFile, overwrite = true)
            tmp.delete()
        }
        true
    }.getOrDefault(false)

    private fun writeNormalizedPng(normalized: Bitmap, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            normalized.compress(Bitmap.CompressFormat.PNG, 92, fos)
        }
        if (!normalized.isRecycled) normalized.recycle()
    }

    private fun decodeSampledFromUri(context: Context, uri: Uri, maxReadSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val maxDim = max(bounds.outWidth, bounds.outHeight)
        while (maxDim / sample > maxReadSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
    }

    private fun decodeSampledFromFile(file: File, maxReadSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val maxDim = max(bounds.outWidth, bounds.outHeight)
        while (maxDim / sample > maxReadSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    private fun renderCenterLetterbox(source: Bitmap, box: Int): Bitmap {
        val w = source.width.toFloat().coerceAtLeast(1f)
        val h = source.height.toFloat().coerceAtLeast(1f)
        val scale = min(box / w, box / h)
        val nw = max(1, (w * scale).toInt().coerceAtMost(box))
        val nh = max(1, (h * scale).toInt().coerceAtMost(box))
        val scaled = Bitmap.createScaledBitmap(source, nw, nh, true)
        val out = Bitmap.createBitmap(box, box, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(
            scaled,
            (box - scaled.width) / 2f,
            (box - scaled.height) / 2f,
            paint,
        )
        if (!scaled.isRecycled) scaled.recycle()
        if (!source.isRecycled) source.recycle()
        return out
    }
}
