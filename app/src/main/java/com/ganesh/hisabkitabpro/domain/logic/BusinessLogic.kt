package com.ganesh.hisabkitabpro.domain.logic

/**
 * HISABKITAB PRO - BUSINESS LOGIC ENGINE
 * Implementation of Blueprint Rule 5.
 */
object BusinessLogic {

    /**
     * 🔔 5. REMINDER LOGIC
     * Returns true if a reminder should be sent.
     */
    fun shouldSendReminder(balance: Long, lastDays: Int, enabled: Boolean = true): Boolean {
        // balance > 0 (in paise) and last transaction/payment was more than 7 days ago
        return enabled && balance > 0 && lastDays > 7
    }

    /**
     * 🧠 5. AI RISK LOGIC
     * Calculates risk score based on balance and delay.
     */
    fun calculateRisk(balancePaise: Long, lateDays: Int): Int {
        val balanceRupees = balancePaise / 100
        return (balanceRupees / 1000).toInt() + lateDays
    }
}
