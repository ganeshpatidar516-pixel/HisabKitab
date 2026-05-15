package com.ganesh.hisabkitabpro.domain.support

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppSender

object HelpSupportActions {

    fun openVideoGuide(context: Context, url: String): Boolean {
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            )
            true
        } catch (_: Exception) {
            Toast.makeText(
                AppLocaleManager.wrapContext(context),
                AppLocaleManager.wrapContext(context).getString(R.string.help_unable_open_link),
                Toast.LENGTH_SHORT
            ).show()
            false
        }
    }

    /**
     * Opens email composer for a support ticket. Falls back to WhatsApp if no mail app handles the intent.
     */
    fun submitSupportTicket(
        context: Context,
        supportEmail: String,
        subject: String,
        body: String
    ): Boolean {
        val trimmed = supportEmail.trim()
        val waBody = context.getString(R.string.support_ticket_whatsapp_body_template, subject, body)
        if (trimmed.isEmpty()) {
            return WhatsAppSender.openSupportChat(context, waBody)
        }
        val mailUri = Uri.parse(
            "mailto:${Uri.encode(trimmed)}?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
        )
        val mailIntent = Intent(Intent.ACTION_SENDTO, mailUri)
        return try {
            context.startActivity(Intent.createChooser(mailIntent, null))
            true
        } catch (_: Exception) {
            WhatsAppSender.openSupportChat(context, waBody)
        }
    }
}
