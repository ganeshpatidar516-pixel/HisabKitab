package com.ganesh.hisabkitabpro.domain.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.util.AdaptiveMessaging
import java.io.File

object WhatsAppSender {

    /** HisabKitab support — wa.me digits only (country + national number, no + or spaces). */
    const val SUPPORT_WA_ME_DIGITS = "916367293391"

    /**
     * Opens WhatsApp (or WhatsApp Business) chat with official support via [wa.me](https://wa.me).
     */
    fun openSupportChat(context: Context, prefilledMessage: String? = null): Boolean {
        val uri = if (prefilledMessage.isNullOrBlank()) {
            Uri.parse("https://wa.me/$SUPPORT_WA_ME_DIGITS")
        } else {
            Uri.parse("https://wa.me/$SUPPORT_WA_ME_DIGITS?text=${Uri.encode(prefilledMessage)}")
        }
        val packages = listOf("com.whatsapp", "com.whatsapp.w4b")
        for (pkg in packages) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
                )
                return true
            } catch (_: Exception) {
            }
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                AppLocaleManager.wrapContext(context).getString(R.string.whatsapp_not_installed),
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * Text balance reminder — uses saved app language ([AppLocaleManager]) and optional
     * [BusinessProfile] footer (phone, UPI, etc.) when provided.
     */
    fun sendPaymentReminder(context: Context, phone: String, amount: Double, profile: BusinessProfile? = null): Boolean {
        val message = AdaptiveMessaging.getSimplePaymentReminder(context, amount, profile)
        return sendTextReminder(context, phone, message)
    }

    /**
     * Sends a payment reminder on WhatsApp (with PDF attachment when used via overload).
     */
    fun sendReminderWithPdf(context: Context, phone: String, message: String, pdfFile: File): Boolean {
        return sendReminderWithAttachment(
            context = context,
            phone = phone,
            message = message,
            file = pdfFile,
            mimeType = "application/pdf"
        )
    }

    fun sendReminderWithImage(context: Context, phone: String, message: String, imageFile: File): Boolean {
        return sendReminderWithAttachment(
            context = context,
            phone = phone,
            message = message,
            file = imageFile,
            mimeType = "image/*"
        )
    }

    private fun sendReminderWithAttachment(
        context: Context,
        phone: String,
        message: String,
        file: File,
        mimeType: String
    ): Boolean {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra("jid", "91$phone@s.whatsapp.net")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            // Fallback to regular text if PDF attachment fails
            return sendTextReminder(context, phone, message)
        }
    }

    fun sendTextReminder(context: Context, phone: String, message: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=91$phone&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
            return true
        } catch (e: Exception) {
            Toast.makeText(context, AppLocaleManager.wrapContext(context).getString(R.string.whatsapp_not_installed), Toast.LENGTH_SHORT).show()
            return false
        }
    }
}
