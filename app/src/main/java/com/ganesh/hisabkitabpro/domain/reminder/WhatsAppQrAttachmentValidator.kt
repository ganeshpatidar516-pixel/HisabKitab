package com.ganesh.hisabkitabpro.domain.reminder

import android.graphics.BitmapFactory
import android.util.Log
import java.io.File

/**
 * Validates the QR attachment *before* attempting to render a showcase card or attach it
 * to WhatsApp.
 *
 * Note: This is a "safety + integrity" validator (file exists + decode bounds sane).
 * We intentionally avoid full QR decode here to keep performance predictable.
 */
object WhatsAppQrAttachmentValidator {

    fun validateQrImageFileOrReason(qrFile: File?): String? {
        if (qrFile == null) return "qr_missing"
        if (!qrFile.exists()) return "qr_file_not_found"
        if (!qrFile.isFile) return "qr_not_regular_file"
        if (qrFile.length() < 2048L) return "qr_file_too_small"

        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(qrFile.absolutePath, opts)
            val w = opts.outWidth
            val h = opts.outHeight
            when {
                w <= 0 || h <= 0 -> "qr_decode_bounds_failed"
                w < 120 || h < 120 -> "qr_decode_too_small"
                else -> null
            }
        } catch (t: Throwable) {
            Log.e("BusinessCardEngine", "QR validation failed: ${qrFile.absolutePath}", t)
            "qr_validation_exception"
        }
    }
}

