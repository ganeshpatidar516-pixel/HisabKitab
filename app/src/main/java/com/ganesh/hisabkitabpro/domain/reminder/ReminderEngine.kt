package com.ganesh.hisabkitabpro.domain.reminder

import android.content.Context
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Reminder
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

object ReminderEngine {

    /** Generates reminder rows for customers with high outstanding balance (auto reminders). */
    fun generateSmartReminders(context: Context, customers: List<Customer>): List<Reminder> {
        val lc = AppLocaleManager.wrapContext(context)
        val fmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return customers.filter { it.balanceCache > 200000 }.map { customer -> // 200000 Paise = 2000 Rupees
            val amountStr = fmt.format(customer.balanceCache / 100.0)
            Reminder(
                id = UUID.randomUUID().toString(),
                customerId = customer.id,
                customerName = customer.name,
                amount = customer.balanceCache / 100.0,
                dueDate = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000), // 7 days ahead
                message = lc.getString(R.string.reminder_auto_pending_message, customer.name, amountStr),
                status = "PENDING"
            )
        }
    }
}
