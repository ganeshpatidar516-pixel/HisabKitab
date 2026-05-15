package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.ReminderCounterpartyKind
import com.ganesh.hisabkitabpro.data.local.ReminderEntity
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Keeps at most one pending [ReminderEntity] per customer in sync with [Customer.balanceCache].
 * Called after ledger transactions update the cached balance (customer owes when balance > 0).
 */
object CustomerPaymentReminderScheduler {
    private const val TAG = "PaymentReminderSync"

    suspend fun syncAfterCustomerBalanceChange(context: Context, database: AppDatabase, customerId: Long) {
        try {
            val customerDao = database.customerDao()
            val reminderDao = database.reminderDao()
            reminderDao.deletePendingUnsentForCustomer(customerId)
            val customer = customerDao.getCustomerById(customerId) ?: return
            if (customer.balanceCache <= 0L) {
                Log.d(TAG, "Cleared pending reminders: customer balance settled")
                return
            }
            val now = System.currentTimeMillis()
            val dueMillis = customer.nextReminderDate?.takeIf { it > now }
                ?: (now + TimeUnit.DAYS.toMillis(7))
            val lc = AppLocaleManager.wrapContext(context.applicationContext)
            val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            val amountStr = fmt.format(customer.balanceCache / 100.0)
            val message = lc.getString(R.string.reminder_auto_pending_message, customer.name, amountStr)
            reminderDao.insertReminder(
                ReminderEntity(
                    customerId = customerId,
                    counterpartyKind = ReminderCounterpartyKind.CUSTOMER,
                    partyId = 0L,
                    message = message,
                    scheduledAt = dueMillis,
                    isSent = false,
                    priority = "NORMAL",
                    type = "PAYMENT",
                    lastEscalationTier = 0
                )
            )
            Log.d(TAG, "Inserted PAYMENT reminder for customer with positive balance")
        } catch (e: Exception) {
            Log.w(TAG, "syncAfterCustomerBalanceChange failed", e)
        }
    }
}
