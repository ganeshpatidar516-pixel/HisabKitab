package com.ganesh.hisabkitabpro.addon.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ganesh.hisabkitabpro.MainActivity

internal object ReminderNotificationHelper {
    const val CHANNEL_ID = "hisab_reminder_engine_v2"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Payment reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        ch.description = "HisabKitab Pro reminder escalations"
        mgr.createNotificationChannel(ch)
    }

    fun notifyEscalation(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        customerPhoneDigits: String?,
        whatsAppBody: String
    ) {
        ensureChannel(context)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notificationId,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPi)
            .setAutoCancel(true)

        ReminderWhatsAppDispatcher.appendAction(context, builder, notificationId + 10_000, customerPhoneDigits, whatsAppBody)
        ReminderSmsDispatcher.appendAction(context, builder, notificationId + 20_000, customerPhoneDigits, whatsAppBody)

        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(notificationId, builder.build())
    }
}
