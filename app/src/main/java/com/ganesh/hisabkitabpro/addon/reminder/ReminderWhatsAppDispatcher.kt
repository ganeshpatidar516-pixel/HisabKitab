package com.ganesh.hisabkitabpro.addon.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat

/**
 * Does not auto-launch from background workers — adds a notification action only.
 */
internal object ReminderWhatsAppDispatcher {

    fun appendAction(
        context: Context,
        builder: NotificationCompat.Builder,
        requestCode: Int,
        phoneDigits: String?,
        message: String
    ) {
        val digits = phoneDigits?.filter { it.isDigit() }?.trim().orEmpty()
        if (digits.isEmpty()) return
        val uri = Uri.parse(
            "https://wa.me/$digits?text=${Uri.encode(message)}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(
            android.R.drawable.ic_menu_send,
            "WhatsApp",
            pi
        )
    }
}
