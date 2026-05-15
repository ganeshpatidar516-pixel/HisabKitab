package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.ReminderCounterpartyKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Periodic / one-shot: escalates pending reminders (T-0 friendly → T+3 firm → T+7 urgent).
 * Uses [ReminderEntity.scheduledAt] as due instant; does not alter ledger balances.
 */
class ReminderEscalationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val tag = "ReminderEscalationWorker"
        try {
            if (!ReminderAutomationPrefs.isAutoPilotEnabled(applicationContext)) {
                ReminderAutomationPrefs.setLastGlobalSkipReason(
                    applicationContext,
                    "Auto pilot disabled from Settings"
                )
                Log.d(tag, "Auto-pilot disabled; skipping run")
                return@withContext Result.success()
            }
            val lc = AppLocaleManager.wrapContext(applicationContext)
            val db = AppDatabase.getDatabase(applicationContext)
            val now = System.currentTimeMillis()
            val dayMs = TimeUnit.DAYS.toMillis(1)
            val list = db.reminderDao().getDueRemindersNotSent(now)
            val customerDao = db.customerDao()
            val partyDao = db.partyDao()
            for (r in list) {
                val isPartySupplier = r.counterpartyKind == ReminderCounterpartyKind.PARTY_SUPPLIER
                if (isPartySupplier) {
                    if (r.partyId <= 0L) continue
                    if (!ReminderAutomationPrefs.isSupplierPartyReminderEnabled(applicationContext, r.partyId)) {
                        ReminderAutomationPrefs.setLastSupplierPartySkipReason(
                            applicationContext,
                            r.partyId,
                            "Supplier auto reminder disabled"
                        )
                        continue
                    }
                    if (ReminderAutomationPrefs.isSupplierPartyAutoPaused(applicationContext, r.partyId)) {
                        ReminderAutomationPrefs.setLastSupplierPartySkipReason(
                            applicationContext,
                            r.partyId,
                            "Supplier reminder paused"
                        )
                        continue
                    }
                } else {
                    if (r.customerId <= 0L) continue
                    if (!ReminderAutomationPrefs.isCustomerReminderEnabled(applicationContext, r.customerId)) {
                        ReminderAutomationPrefs.setLastCustomerSkipReason(
                            applicationContext,
                            r.customerId,
                            "Customer auto reminder disabled"
                        )
                        continue
                    }
                    if (ReminderAutomationPrefs.isCustomerAutoPaused(applicationContext, r.customerId)) {
                        ReminderAutomationPrefs.setLastCustomerSkipReason(
                            applicationContext,
                            r.customerId,
                            "Customer reminder paused"
                        )
                        continue
                    }
                }

                val daysLate = ((now - r.scheduledAt) / dayMs).toInt().coerceAtLeast(0)
                val desiredTier = when {
                    daysLate >= 7 -> 3
                    daysLate >= 3 -> 2
                    else -> 1
                }
                if (desiredTier <= r.lastEscalationTier) continue

                val name: String
                val phone: String?
                val netDuePaise: Long
                val attempts: Int
                val preferredChannel: AutoReminderChannel?
                val auditSubject: String

                if (isPartySupplier) {
                    val party = partyDao.getPartyById(r.partyId) ?: continue
                    if (!party.isSupplier) continue
                    name = party.name.trim().ifBlank { "Supplier" }
                    phone = party.phone
                    netDuePaise = party.totalBalance
                    attempts = ReminderAutomationPrefs.getReminderAttemptsSupplierParty(applicationContext, r.partyId)
                    preferredChannel = ReminderAutomationPrefs.getPreferredChannelSupplierParty(applicationContext, r.partyId)
                    auditSubject = "partySupplierId=${r.partyId}"
                } else {
                    val customer = customerDao.getCustomerById(r.customerId)
                    name = customer?.name?.trim().orEmpty().ifBlank { "Customer" }
                    phone = customer?.phone
                    netDuePaise = customer?.balanceCache ?: 0L
                    attempts = ReminderAutomationPrefs.getReminderAttempts(applicationContext, r.customerId)
                    preferredChannel = ReminderAutomationPrefs.getPreferredChannel(applicationContext, r.customerId)
                    auditSubject = "customerId=${r.customerId}"
                }

                val plan = ReminderBehaviorEngine.selectPlan(
                    daysOverdue = daysLate,
                    netDuePaise = netDuePaise,
                    previousAttempts = attempts,
                    preferredChannel = preferredChannel
                )

                val (title, body) = when (desiredTier) {
                    1 -> lc.getString(R.string.notif_reminder_due_title) to
                        lc.getString(R.string.notif_reminder_due_body, name, r.message)
                    2 -> lc.getString(R.string.notif_payment_overdue_title) to
                        lc.getString(R.string.notif_payment_overdue_body, name, r.message)
                    else -> lc.getString(R.string.notif_urgent_settlement_title) to
                        lc.getString(R.string.notif_urgent_settlement_body, name, r.message)
                }
                val adaptiveSuffix = " [AUTO ${plan.tone.name}/${plan.channel.name}]"
                Log.d(
                    tag,
                    "Escalation tier=$desiredTier reminderId=${r.id} kind=${r.counterpartyKind} " +
                        "${if (isPartySupplier) "party=${r.partyId}" else "customer=${r.customerId}"} " +
                        "daysLate=$daysLate plan=${plan.tone}/${plan.channel}"
                )

                val notifId = r.id * 4 + desiredTier
                ReminderNotificationHelper.notifyEscalation(
                    applicationContext,
                    notifId,
                    title,
                    body + adaptiveSuffix,
                    phone,
                    body + adaptiveSuffix
                )
                db.reminderDao().updateReminder(r.copy(lastEscalationTier = desiredTier))
                db.auditLogDao().insert(
                    AuditLogEntry(
                        entityType = "REMINDER",
                        entityId = r.id.toLong(),
                        action = "AUTO_ESCALATION_TIER_${desiredTier}_${plan.tone}_${plan.channel}",
                        detail = "$auditSubject,attempts=$attempts,phone=${phone ?: "NA"}"
                    )
                )
            }

            // 24h follow-up prompt: manual remind sent but payment still pending.
            customerDao.getDebtors().forEach { debtor ->
                val manualAt = ReminderAutomationPrefs.getLastManualReminderAt(applicationContext, debtor.id)
                if (manualAt <= 0L) return@forEach
                val crossed24h = now - manualAt >= TimeUnit.HOURS.toMillis(24)
                val alreadyNotified = ReminderAutomationPrefs.isFollowUpNotified(applicationContext, debtor.id, manualAt)
                if (!crossed24h || alreadyNotified) return@forEach

                val phone = debtor.phone.filter { it.isDigit() }.ifBlank { null }
                val followUpBody = lc.getString(
                    R.string.notif_followup_suggested_body,
                    debtor.name
                )
                val notifId = (debtor.id % 100000).toInt() + 70_000
                ReminderNotificationHelper.notifyEscalation(
                    applicationContext,
                    notifId,
                    lc.getString(R.string.notif_followup_suggested_title),
                    followUpBody,
                    phone,
                    followUpBody
                )
                ReminderAutomationPrefs.markFollowUpNotified(applicationContext, debtor.id, manualAt)
                db.auditLogDao().insert(
                    AuditLogEntry(
                        entityType = "REMINDER",
                        entityId = debtor.id,
                        action = "FOLLOW_UP_24H_SUGGESTED",
                        detail = "customerId=${debtor.id},balance=${debtor.balanceCache}"
                    )
                )
            }
            Log.d(tag, "Run completed (due reminders + follow-up scan)")
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "Auto-pilot reminder run failed", e)
            Result.retry()
        }
    }
}
