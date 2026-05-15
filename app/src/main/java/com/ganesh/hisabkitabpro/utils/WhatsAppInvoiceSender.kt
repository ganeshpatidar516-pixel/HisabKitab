package com.ganesh.hisabkitabpro.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * NOTE: file lives under `utils/` (plural) but declares package
 * `com.ganesh.hisabkitabpro.util` (singular) — pre-existing inconsistency.
 *
 * Has zero callers in `app/src/main`. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt:49797`).
 *
 * The LIVE invoice-share path uses [com.ganesh.hisabkitabpro.util.WhatsAppBillSender]
 * — that path is intent-only, FileProvider-safe, and supports both WhatsApp and
 * WhatsApp Business with graceful fallback.
 */
@Deprecated(
    message = "Legacy invoice sender. Use com.ganesh.hisabkitabpro.util.WhatsAppBillSender (live).",
    replaceWith = ReplaceWith("com.ganesh.hisabkitabpro.util.WhatsAppBillSender"),
    level = DeprecationLevel.WARNING
)
class WhatsAppInvoiceSender {

    fun sendInvoice(
        context: Context,
        phoneNumber: String,
        pdfFile: File
    ) {

        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.setPackage("com.whatsapp")

        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.putExtra(
            "jid",
            "$phoneNumber@s.whatsapp.net"
        )

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        context.startActivity(intent)
    }
}