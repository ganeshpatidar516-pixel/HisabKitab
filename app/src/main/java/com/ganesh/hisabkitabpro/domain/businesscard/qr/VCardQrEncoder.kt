package com.ganesh.hisabkitabpro.domain.businesscard.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Wraps ZXing into a tiny façade that always emits high error-correction QR matrices,
 * which are required to keep the code scannable when overlaid onto premium card surfaces
 * (gradients, foil, dark backgrounds).
 *
 * The encoder is intentionally vCard-agnostic — any payload (including the RFC 6350
 * vCard string built by [com.ganesh.hisabkitabpro.domain.businesscard.vcard.VCardEncoder])
 * can be passed in.
 */
object VCardQrEncoder {

    fun encode(payload: String, sizePx: Int = 720): Bitmap? {
        if (payload.isBlank()) return null
        val side = sizePx.coerceAtLeast(128)
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to 1,
            )
            val matrix = MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, side, side, hints)
            val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
            for (x in 0 until matrix.width) {
                for (y in 0 until matrix.height) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
