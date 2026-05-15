package com.ganesh.hisabkitabpro.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ganesh.hisabkitabpro.MainActivity
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val lc = AppLocaleManager.wrapContext(context)
        val customerName = intent.getStringExtra("CUSTOMER_NAME")
            ?: lc.getString(R.string.notif_default_customer)
        val amount = intent.getStringExtra("AMOUNT") ?: ""

        showNotification(context, lc, customerName, amount)
    }

    private fun showNotification(context: Context, lc: Context, name: String, amount: String) {
        val channelId = "payment_reminders"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = lc.getString(R.string.notif_channel_payment_reminders)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val title = lc.getString(R.string.notif_payment_due_title, name)
        val body = lc.getString(R.string.notif_payment_due_body, amount, name)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(name.hashCode(), notification)
    }
}
