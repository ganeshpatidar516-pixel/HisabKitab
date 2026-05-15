package com.ganesh.hisabkitabpro.domain.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Resolves business profile image paths for PDF / Canvas rendering.
 * Supports app-private absolute files (primary), [file://], and [content://] URIs.
 *
 * Raster formats (PNG/JPEG/WebP via decoder) are supported. **SVG** is not decoded here;
 * convert to PNG on import or add a dedicated SVG rasterizer if product requires it.
 */
object ProfileBitmapLoader {

    /** Max raster side for invoice / statement / ledger PDF embeds (limits OOM on large camera logos). */
    const val PDF_EMBED_MAX_SIDE_PX = 512

    fun loadBitmapForPdfEmbed(context: Context, pathOrUri: String?): Bitmap? =
        loadBitmapMaxSide(context, pathOrUri, PDF_EMBED_MAX_SIDE_PX)

    fun loadBitmap(context: Context, pathOrUri: String?): Bitmap? {
        val raw = pathOrUri?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val file = File(raw)
        if (file.exists() && file.isFile) {
            return BitmapFactory.decodeFile(file.absolutePath)?.takeIf { !it.isRecycled }
        }
        return try {
            val uri = when {
                raw.startsWith("content://", ignoreCase = true) -> Uri.parse(raw)
                raw.startsWith("file://", ignoreCase = true) -> Uri.parse(raw)
                else -> null
            } ?: return null
            context.applicationContext.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }?.takeIf { !it.isRecycled }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Like [loadBitmap] but decodes with a **max side** cap (down-sampling) to limit RAM — used for
     * business-card sheets where the same logo is decoded many times next to high-DPI card renders.
     */
    fun loadBitmapMaxSide(context: Context, pathOrUri: String?, maxSidePx: Int): Bitmap? {
        val raw = pathOrUri?.trim().orEmpty()
        if (raw.isEmpty() || maxSidePx < 1) return null
        val app = context.applicationContext
        val file = File(raw)
        if (file.exists() && file.isFile) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sample = maxOf(bounds.outWidth / maxSidePx, bounds.outHeight / maxSidePx, 1)
            val decode = BitmapFactory.Options().apply { inSampleSize = sample }
            return BitmapFactory.decodeFile(file.absolutePath, decode)?.takeIf { !it.isRecycled }
        }
        val uri = when {
            raw.startsWith("content://", ignoreCase = true) -> Uri.parse(raw)
            raw.startsWith("file://", ignoreCase = true) -> Uri.parse(raw)
            else -> null
        } ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val sample = maxOf(bounds.outWidth / maxSidePx, bounds.outHeight / maxSidePx, 1)
            app.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }?.takeIf { !it.isRecycled }
        } catch (_: Throwable) {
            null
        }
    }
}
