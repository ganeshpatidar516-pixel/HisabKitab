package com.ganesh.hisabkitabpro.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.payment.BankNotificationParser
import com.ganesh.hisabkitabpro.domain.payment.TransactionMatcher
import com.ganesh.hisabkitabpro.feature.banksettle.BankAutoSettleFeatureToggle
import com.ganesh.hisabkitabpro.receiver.BankAutoSettleSuggestionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * On-device interceptor for bank-credit / SMS-app payment notifications.
 *
 * Compliance boundaries (do NOT relax without re-reviewing Play policy):
 *  1. Package allowlist is checked **before** any notification extras are read.
 *  2. No READ_SMS / RECEIVE_SMS / Telephony.Sms / content://sms is used.
 *  3. Raw notification text is never persisted, logged, or transmitted.
 *  4. Output is a single in-app broadcast → user sees a local suggestion only.
 */
class BankNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val packageName = sbn?.packageName ?: return
        if (packageName == applicationContext.packageName) return

        // Privacy gate #1: drop non-financial sources before touching any extras.
        if (!BankNotificationParser.isAllowedFinancialSource(packageName)) return

        // Privacy gate #2: feature must be explicitly opted in by the merchant.
        val prefs = applicationContext.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
        if (!BankAutoSettleFeatureToggle(prefs).isEnabled()) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val parsedNotification = BankNotificationParser.parseNotification(
            packageName = packageName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty(),
            textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map { it.toString() }
                .orEmpty()
        ) ?: return
        val parsed = BankNotificationParser.toTransactionMatcherInput(parsedNotification)

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val bill = TransactionMatcher.findPendingBillMatch(db, parsed) ?: return@launch
                val intent = Intent(BankAutoSettleSuggestionReceiver.ACTION).apply {
                    // Defense-in-depth: setClass already makes this explicit, but on
                    // Android 14+ implicit-broadcast scrutiny tightens; pinning the
                    // package guarantees the broadcast can never escape this app's
                    // process even if the action string were ever reused elsewhere.
                    setPackage(applicationContext.packageName)
                    setClass(applicationContext, BankAutoSettleSuggestionReceiver::class.java)
                    putExtra(BankAutoSettleSuggestionReceiver.EXTRA_BILL_ID, bill.id)
                    putExtra(BankAutoSettleSuggestionReceiver.EXTRA_AMOUNT_PAISE, parsed.amountPaise)
                    putExtra(BankAutoSettleSuggestionReceiver.EXTRA_CUSTOMER_ID, bill.customerId)
                    putExtra(BankAutoSettleSuggestionReceiver.EXTRA_BANK_NAME, parsed.bankName)
                }
                sendBroadcast(intent)
            } catch (e: Exception) {
                // Never log raw notification content — only the failure mode.
                Log.e(TAG, "match pipeline failed: ${e::class.java.simpleName}")
            }
        }
    }

    /**
     * The system or Play Protect can unbind notification listeners. Without a
     * rebind request the feature would silently die until a reboot, even though
     * the user already granted notification access. We re-request the bind so
     * the merchant doesn't have to re-toggle the system permission.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                requestRebind(ComponentName(applicationContext, BankNotificationListener::class.java))
            }
        }
    }

    override fun onDestroy() {
        runCatching { serviceScope.cancel() }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "BankAutoSettle"
    }
}
