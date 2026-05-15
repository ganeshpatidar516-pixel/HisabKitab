package com.ganesh.hisabkitabpro.addon.reminder

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat

/** Optional SMS compose — user-triggered via notification action only. */
internal object ReminderSmsDispatcher {

    fun appendAction(
        context: Context,
        builder: NotificationCompat.Builder,
        requestCode: Int,
        phoneDigits: String?,
        message: String
    ) {
        val digits = phoneDigits?.filter { it.isDigit() }?.trim().orEmpty()
        if (digits.isEmpty()) return
        val uri = Uri.parse("smsto:$digits")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pi = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.sym_action_chat, "SMS", pi)
    }
}
