package com.ganesh.hisabkitabpro.domain.engine

import com.ganesh.hisabkitabpro.domain.model.Customer

class ReminderEngine {

    fun shouldSendReminder(
        customer: Customer,
        currentTime: Long
    ): Boolean {

        val oneDayMillis = 24L * 60L * 60L * 1000L

        return customer.balanceCache > 0 && (customer.lastTransactionDate ?: 0L) > 0 && currentTime > oneDayMillis
    }

    fun createReminderMessage(customer: Customer): String {

        return "Hello ${customer.name},\n" +
                "Your balance due is ₹${customer.balanceCache / 100.0}.\n" +
                "Please arrange payment. Thank you."
    }
}
