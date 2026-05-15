package com.ganesh.hisabkitabpro.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ganesh.hisabkitabpro.R

/**
 * In-app hook for bank match suggestions (explicit broadcast, package-scoped).
 * Shows a non-invasive notification — user confirms settlement in-app; no auto-posting.
 */
class BankAutoSettleSuggestionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val billId = intent.getLongExtra(EXTRA_BILL_ID, -1L)
        val amountPaise = intent.getLongExtra(EXTRA_AMOUNT_PAISE, -1L)
        val bankName = intent.getStringExtra(EXTRA_BANK_NAME)?.takeIf { it.isNotBlank() }
        if (billId < 0L || amountPaise < 0L) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.bank_match_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(ch)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.bank_match_notification_title))
            .setContentText(
                if (bankName == null) {
                    context.getString(
                        R.string.bank_match_notification_body,
                        amountPaise / 100.0,
                        billId.toString()
                    )
                } else {
                    context.getString(
                        R.string.bank_match_notification_body_with_bank,
                        amountPaise / 100.0,
                        bankName,
                        billId.toString()
                    )
                }
            )
            .setAutoCancel(true)
            .build()

        runCatching {
            nm.notify((billId xor amountPaise).toInt() and 0x7fffffff, notification)
        }
    }

    companion object {
        const val ACTION = "com.ganesh.hisabkitabpro.ACTION_BANK_AUTO_SETTLE_SUGGESTION"
        const val EXTRA_BILL_ID = "billId"
        const val EXTRA_AMOUNT_PAISE = "amountPaise"
        const val EXTRA_CUSTOMER_ID = "customerId"
        const val EXTRA_BANK_NAME = "bankName"
        const val CHANNEL_ID = "hisabkitab_bank_match_v1"
    }
}
