package com.ganesh.hisabkitabpro.service

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * **Phase 6 — activity trampoline:** launches the WhatsApp `ACTION_SEND` chooser from a
 * **foreground Activity** context so background activity launch (BAL) restrictions are less
 * likely to block bulk reminder sends that originate from [BulkReminderService].
 *
 * Finishes immediately after handing off to WhatsApp (or on failure).
 */
class BulkWhatsAppTrampolineActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val phone = intent.getStringExtra(EXTRA_PHONE).orEmpty()
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val qrPath = intent.getStringExtra(EXTRA_QR_PATH)
        if (phone.isEmpty() || message.isEmpty()) {
            finish()
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(Intent.EXTRA_TEXT, message)
            if (!qrPath.isNullOrEmpty()) {
                val file = File(qrPath)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        this@BulkWhatsAppTrampolineActivity,
                        "${packageName}.provider",
                        file
                    )
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
            } else {
                type = "text/plain"
            }
        }
        runCatching {
            startActivity(send)
        }.onFailure { e ->
            Log.w(TAG, "whatsapp_trampoline_failed type=${e::class.java.simpleName}", e)
            Toast.makeText(
                this,
                "Could not open WhatsApp. Install it or try again when the app is open.",
                Toast.LENGTH_LONG
            ).show()
        }
        finish()
    }

    companion object {
        private const val TAG = "BulkWhatsAppTramp"
        const val EXTRA_PHONE = "bulk_wa_phone"
        const val EXTRA_MESSAGE = "bulk_wa_message"
        const val EXTRA_QR_PATH = "bulk_wa_qr_path"
    }
}
