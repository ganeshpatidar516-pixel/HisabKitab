package com.ganesh.hisabkitabpro.domain.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.core.storage.StorageSpaceGuard
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** P3 — result of copying a gallery [Uri] into app OCR cache (size-capped, temp-file safe). */
sealed class OcrGalleryImportCopy {
    data class Ok(val file: File) : OcrGalleryImportCopy()
    data object TooLarge : OcrGalleryImportCopy()
    data object Failed : OcrGalleryImportCopy()
}

/** P4 — [decodeJpegFileForOcr] outcome so UI can distinguish decode limits vs unreadable files. */
sealed class OcrDecodeForOcrResult {
    data class Decoded(val bitmap: Bitmap) : OcrDecodeForOcrResult()
    data object ExceedsDecodeLimits : OcrDecodeForOcrResult()
    data object Unreadable : OcrDecodeForOcrResult()
}

/**
 * Wave 1 — memory-safe decode / downscale for on-device bill OCR.
 * Wave 2 (P2) — applies JPEG/TIFF EXIF **rotation** so gallery/camera files match upright text for ML Kit.
 * Wave 3 (P3) — gallery import **byte cap** + decode **dimension cap** to protect low-RAM devices.
 * Wave 4 (P4) — [OcrDecodeForOcrResult] surfaces decode-limit vs unreadable for clearer UI copy.
 * Keeps the long edge bounded so full-resolution camera JPEGs do not allocate huge single allocations.
 */
object OcrImageLoader {

    /** Long-edge cap for ML Kit text on receipts / bills. */
    const val DEFAULT_MAX_EDGE_PX: Int = 2048

    /** P3 — refuse to copy more than this many bytes from a gallery [Uri] (single allocation bound). */
    const val MAX_GALLERY_IMPORT_BYTES: Long = 32L * 1024L * 1024L

    /** P3 — reject decode if reported pixel count exceeds this (avoids pathological huge images). */
    const val MAX_IMAGE_PIXEL_COUNT: Long = 35_000_000L

    /** P3 — reject decode if either side exceeds this (in addition to pixel count). */
    const val MAX_IMAGE_DIMENSION_PX: Int = 8192

    /**
     * P1+P3 — copy a content [Uri] (e.g. gallery pick) into app OCR cache, enforcing [MAX_GALLERY_IMPORT_BYTES].
     * Caller deletes the file when no longer needed. Deletes partial file on failure or oversize.
     */
    fun copyContentUriToOcrCacheFile(context: Context, uri: Uri): OcrGalleryImportCopy {
        if (!StorageSpaceGuard.hasMinFreeSpace(context, minFreeMb = 48L)) {
            return OcrGalleryImportCopy.Failed
        }
        val dest = File(AppStoragePaths.ocrCacheDir(context), "ocr_import_${System.currentTimeMillis()}.bin")
        return try {
            val reportedLen = runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
            }.getOrDefault(-1L)
            if (reportedLen > MAX_GALLERY_IMPORT_BYTES) {
                OcrTelemetry.event("ocr_gallery_import", mapOf("outcome" to "oversize_stat"))
                return OcrGalleryImportCopy.TooLarge
            }
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return OcrGalleryImportCopy.Failed
            var streamOk = false
            inputStream.use { ins ->
                dest.outputStream().use { outs ->
                    streamOk = copyStreamWithByteCap(ins, outs, MAX_GALLERY_IMPORT_BYTES)
                }
            }
            if (!streamOk) {
                if (dest.exists()) dest.delete()
                OcrTelemetry.event("ocr_gallery_import", mapOf("outcome" to "oversize_stream"))
                return OcrGalleryImportCopy.TooLarge
            }
            if (!dest.exists() || dest.length() == 0L) {
                dest.delete()
                OcrGalleryImportCopy.Failed
            } else {
                OcrGalleryImportCopy.Ok(dest)
            }
        } catch (_: Exception) {
            if (dest.exists()) dest.delete()
            OcrGalleryImportCopy.Failed
        }
    }

    private fun copyStreamWithByteCap(input: InputStream, output: OutputStream, maxBytes: Long): Boolean {
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read == -1) break
            if (read == 0) continue
            if (total + read > maxBytes) return false
            output.write(buffer, 0, read)
            total += read
        }
        return true
    }

    /**
     * Decode a JPEG/PNG (or other [BitmapFactory]-supported image) from disk with
     * [BitmapFactory.Options.inSampleSize], then scale down if the long edge still exceeds [maxEdgePx].
     * Caller **must** [Bitmap.recycle] [OcrDecodeForOcrResult.Decoded.bitmap].
     */
    fun decodeJpegFileForOcr(absolutePath: String, maxEdgePx: Int = DEFAULT_MAX_EDGE_PX): OcrDecodeForOcrResult {
        val started = System.nanoTime()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return OcrDecodeForOcrResult.Unreadable
        val w = bounds.outWidth.toLong()
        val h = bounds.outHeight.toLong()
        if (bounds.outWidth > MAX_IMAGE_DIMENSION_PX || bounds.outHeight > MAX_IMAGE_DIMENSION_PX) {
            OcrTelemetry.event("ocr_decode_bounds", mapOf("outcome" to "dimension_cap"))
            return OcrDecodeForOcrResult.ExceedsDecodeLimits
        }
        if (w * h > MAX_IMAGE_PIXEL_COUNT) {
            OcrTelemetry.event("ocr_decode_bounds", mapOf("outcome" to "pixel_cap"))
            return OcrDecodeForOcrResult.ExceedsDecodeLimits
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxEdgePx)
        }
        val decoded = BitmapFactory.decodeFile(absolutePath, decodeOpts)
            ?: return OcrDecodeForOcrResult.Unreadable
        val oriented = applyExifRotationInPlace(decoded, absolutePath)
        val scaled = scaleDownCopyIfNeeded(oriented, maxEdgePx)
        if (scaled !== oriented) {
            oriented.recycle()
        }
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L
        OcrTelemetry.eventTimed(
            "ocr_decode_ok",
            elapsedMs,
            mapOf("max_edge" to maxEdgePx.toString()),
        )
        return OcrDecodeForOcrResult.Decoded(scaled)
    }

    /**
     * Rotates [bitmap] to EXIF upright orientation when [ExifInterface.rotationDegrees] is non-zero.
     * Recycles [bitmap] when a new rotated bitmap is created. On failure or OOM, returns [bitmap] unchanged.
     */
    internal fun applyExifRotationInPlace(bitmap: Bitmap, absolutePath: String): Bitmap {
        val degrees = try {
            ExifInterface(absolutePath).rotationDegrees
        } catch (_: Exception) {
            0
        }
        if (degrees == 0 || degrees % 360 == 0) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (!bitmap.isRecycled && rotated !== bitmap) {
                bitmap.recycle()
            }
            rotated
        } catch (_: OutOfMemoryError) {
            bitmap
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Returns [source] if already within [maxEdgePx] on the long edge; otherwise a new scaled
     * bitmap. **Never** recycles [source] — caller retains ownership of the original reference.
     */
    fun scaleDownCopyIfNeeded(source: Bitmap, maxEdgePx: Int = DEFAULT_MAX_EDGE_PX): Bitmap {
        val w = source.width
        val h = source.height
        val longSide = max(w, h)
        if (longSide <= maxEdgePx) return source
        val scale = maxEdgePx.toFloat() / longSide.toFloat()
        val nw = (w * scale).roundToInt().coerceAtLeast(1)
        val nh = (h * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        var inSampleSize = 1
        val longSide = max(width, height)
        while (longSide / inSampleSize > maxEdgePx) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }
}
