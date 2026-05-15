package com.ganesh.hisabkitabpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.util.AdaptiveMessaging
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppPaymentShowcaseRenderer
import com.ganesh.hisabkitabpro.domain.reminder.WhatsAppQrAttachmentValidator
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground helper for **bulk WhatsApp reminder** intents.
 *
 * **Phase 6 — trampoline:** each send goes through [BulkWhatsAppTrampolineActivity] so WhatsApp
 * is opened from a **short-lived foreground activity**, improving compatibility with BAL rules
 * versus launching WhatsApp directly from this service.
 */
class BulkReminderService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Initializing Zero-Lag Recovery...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            var whatsAppFailures = 0
            try {
                val db = AppDatabase.getDatabase(applicationContext)

                val debtors = db.customerDao().getDebtors()

                if (debtors.isEmpty()) {
                    updateNotification("No pending dues found.")
                    delay(1000)
                } else {
                    debtors.forEachIndexed { index, customer ->
                        // STEP 1-3: fetch latest business profile + validate QR for this reminder.
                        val latestProfile = db.businessProfileDao().getBusinessProfileOnce()

                        val message = AdaptiveMessaging.getPaymentReminder(
                            applicationContext,
                            customer.name,
                            customer.balanceCache,
                            profile = latestProfile,
                        )

                        val qrFile = latestProfile
                            ?.qrImagePath
                            ?.takeIf { it.isNotBlank() }
                            ?.let { File(it) }
                            ?.takeIf { it.exists() }
                        val qrOk = WhatsAppQrAttachmentValidator.validateQrImageFileOrReason(qrFile) == null

                        val waShowcaseFile = if (qrOk && qrFile != null) {
                            WhatsAppPaymentShowcaseRenderer.renderToCacheFileOrNull(
                                context = applicationContext,
                                profile = latestProfile,
                                customerName = customer.name,
                                amountPaise = customer.balanceCache,
                                qrImageFile = qrFile
                            )?.takeIf { it.exists() && it.length() > 1024L }
                        } else null

                        val ok = sendWhatsAppReminderOnMain(
                            phone = customer.phone ?: "",
                            message = message,
                            qrPath = waShowcaseFile?.absolutePath ?: latestProfile?.qrImagePath
                        )
                        if (!ok) whatsAppFailures++

                        updateNotification("Sending: ${customer.name} (${index + 1}/${debtors.size})")
                        delay(1500)
                    }
                    if (whatsAppFailures > 0) {
                        updateNotification(
                            "Finished with $whatsAppFailures WhatsApp open failure(s). " +
                                "Open HisabKitab when on screen and ensure WhatsApp is installed, then retry."
                        )
                        delay(3500)
                    }
                }

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "bulk_reminder_pass_failed", e)
                runCatching {
                    updateNotification("Bulk reminders stopped: ${e.javaClass.simpleName}")
                    delay(2500)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    /**
     * @return `false` if the trampoline activity could not be started;
     * `true` if skipped (empty phone) or trampoline launched (WhatsApp handoff runs in activity).
     */
    private suspend fun sendWhatsAppReminderOnMain(
        phone: String,
        message: String,
        qrPath: String?
    ): Boolean {
        if (phone.isEmpty()) return true
        return try {
            withContext(Dispatchers.Main) {
                val launch = Intent(applicationContext, BulkWhatsAppTrampolineActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(BulkWhatsAppTrampolineActivity.EXTRA_PHONE, phone)
                    putExtra(BulkWhatsAppTrampolineActivity.EXTRA_MESSAGE, message)
                    if (!qrPath.isNullOrEmpty()) {
                        putExtra(BulkWhatsAppTrampolineActivity.EXTRA_QR_PATH, qrPath)
                    }
                }
                startActivity(launch)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "whatsapp_trampoline_launch_failed type=${e.javaClass.simpleName}", e)
            false
        }
    }

    private fun createNotification(content: String): Notification {
        val channelId = "bulk_reminder_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bulk Reminders",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.bulk_reminder_notif_title))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BulkReminderSvc"
        private const val NOTIFICATION_ID = 1001
    }
}
