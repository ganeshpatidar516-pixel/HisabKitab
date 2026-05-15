package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.ReminderCounterpartyKind
import com.ganesh.hisabkitabpro.data.local.ReminderEntity
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierCreditTermsPrefs
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * One pending [ReminderEntity] per supplier [com.ganesh.hisabkitabpro.domain.model.Party] when payable
 * when [com.ganesh.hisabkitabpro.domain.model.Party.totalBalance] is positive. Uses [SupplierCreditTermsPrefs] for first [scheduledAt].
 */
object SupplierPartyReminderScheduler {
    private const val TAG = "SupplierPartyReminder"

    suspend fun syncAfterPartySupplierBalanceChange(context: Context, database: AppDatabase, partyId: Long) {
        try {
            val partyDao = database.partyDao()
            val reminderDao = database.reminderDao()
            reminderDao.deletePendingUnsentForPartySupplier(partyId)
            val party = partyDao.getPartyById(partyId) ?: return
            if (!party.isSupplier) return
            if (party.totalBalance <= 0L) {
                Log.d(TAG, "Cleared pending reminders: supplier payable settled")
                return
            }
            val now = System.currentTimeMillis()
            val termDays = SupplierCreditTermsPrefs.getTermDays(context.applicationContext, partyId)
            val dueMillis = now + TimeUnit.DAYS.toMillis(termDays.toLong())
            val lc = AppLocaleManager.wrapContext(context.applicationContext)
            val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val amountStr = fmt.format(party.totalBalance / 100.0)
            val message = lc.getString(R.string.reminder_auto_pending_message, party.name, amountStr)
            reminderDao.insertReminder(
                ReminderEntity(
                    customerId = 0L,
                    counterpartyKind = ReminderCounterpartyKind.PARTY_SUPPLIER,
                    partyId = partyId,
                    message = message,
                    scheduledAt = dueMillis,
                    isSent = false,
                    priority = "NORMAL",
                    type = "PAYMENT",
                    lastEscalationTier = 0
                )
            )
            Log.d(TAG, "Inserted PAYMENT reminder for supplier payable")
        } catch (e: Exception) {
            Log.w(TAG, "syncAfterPartySupplierBalanceChange failed", e)
        }
    }
}
