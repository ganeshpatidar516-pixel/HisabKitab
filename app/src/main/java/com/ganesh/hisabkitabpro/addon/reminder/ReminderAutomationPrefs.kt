package com.ganesh.hisabkitabpro.addon.reminder

import android.content.Context
import java.util.concurrent.TimeUnit

object ReminderAutomationPrefs {
    private const val PREFS_NAME = "ahre_reminder_prefs"
    private const val KEY_AUTOPILOT_ENABLED = "autopilot_enabled"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isAutoPilotEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_AUTOPILOT_ENABLED, true)
    }

    fun setAutoPilotEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTOPILOT_ENABLED, enabled).apply()
    }

    fun setLastGlobalSkipReason(context: Context, reason: String) {
        prefs(context).edit()
            .putString("last_skip_reason_global", reason)
            .putLong("last_skip_reason_global_at", System.currentTimeMillis())
            .apply()
    }

    fun getLastGlobalSkipReason(context: Context): String? =
        prefs(context).getString("last_skip_reason_global", null)

    fun setManualPauseForCustomer(context: Context, customerId: Long, days: Int = 7) {
        val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        prefs(context).edit().putLong("pause_until_customer_$customerId", until).apply()
    }

    fun setCustomerPauseUntil(context: Context, customerId: Long, untilMillis: Long) {
        prefs(context).edit().putLong("pause_until_customer_$customerId", untilMillis).apply()
    }

    fun isCustomerAutoPaused(context: Context, customerId: Long): Boolean {
        val until = prefs(context).getLong("pause_until_customer_$customerId", 0L)
        return until > System.currentTimeMillis()
    }

    fun getCustomerPauseUntil(context: Context, customerId: Long): Long {
        return prefs(context).getLong("pause_until_customer_$customerId", 0L)
    }

    fun markManualReminderSent(context: Context, customerId: Long, transactionId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong("manual_sent_customer_${customerId}_tx_${transactionId}", now)
            .putLong("manual_last_customer_$customerId", now)
            .apply()
    }

    fun wasManualReminderSent(context: Context, customerId: Long, transactionId: Long): Boolean {
        return prefs(context).getLong("manual_sent_customer_${customerId}_tx_${transactionId}", 0L) > 0L
    }

    fun getLastManualReminderAt(context: Context, customerId: Long): Long {
        return prefs(context).getLong("manual_last_customer_$customerId", 0L)
    }

    fun markFollowUpNotified(context: Context, customerId: Long, manualReminderAt: Long) {
        prefs(context).edit().putLong("follow_up_notified_customer_$customerId", manualReminderAt).apply()
    }

    fun isFollowUpNotified(context: Context, customerId: Long, manualReminderAt: Long): Boolean {
        return prefs(context).getLong("follow_up_notified_customer_$customerId", 0L) >= manualReminderAt
    }

    fun isCustomerReminderEnabled(context: Context, customerId: Long): Boolean {
        return prefs(context).getBoolean("auto_enabled_customer_$customerId", true)
    }

    fun setCustomerReminderEnabled(context: Context, customerId: Long, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_enabled_customer_$customerId", enabled).apply()
    }

    fun setLastCustomerSkipReason(context: Context, customerId: Long, reason: String) {
        prefs(context).edit()
            .putString("last_skip_reason_customer_$customerId", reason)
            .putLong("last_skip_reason_customer_${customerId}_at", System.currentTimeMillis())
            .apply()
    }

    fun getLastCustomerSkipReason(context: Context, customerId: Long): String? =
        prefs(context).getString("last_skip_reason_customer_$customerId", null)

    fun getReminderAttempts(context: Context, customerId: Long): Int {
        return prefs(context).getInt("reminder_attempts_$customerId", 0)
    }

    fun incrementReminderAttempts(context: Context, customerId: Long) {
        val key = "reminder_attempts_$customerId"
        val current = prefs(context).getInt(key, 0)
        prefs(context).edit().putInt(key, current + 1).apply()
    }

    fun resetReminderAttempts(context: Context, customerId: Long) {
        prefs(context).edit().putInt("reminder_attempts_$customerId", 0).apply()
    }

    fun getPreferredChannel(context: Context, customerId: Long): AutoReminderChannel? {
        val value = prefs(context).getString("preferred_channel_$customerId", null) ?: return null
        return runCatching { AutoReminderChannel.valueOf(value) }.getOrNull()
    }

    fun setPreferredChannel(context: Context, customerId: Long, channel: AutoReminderChannel) {
        prefs(context).edit().putString("preferred_channel_$customerId", channel.name).apply()
    }

    // --- Unified supplier party (Party.id); keys never overlap with customer ids. ---

    fun isSupplierPartyReminderEnabled(context: Context, partyId: Long): Boolean {
        return prefs(context).getBoolean("auto_enabled_supplier_party_$partyId", true)
    }

    fun setSupplierPartyReminderEnabled(context: Context, partyId: Long, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_enabled_supplier_party_$partyId", enabled).apply()
    }

    fun setLastSupplierPartySkipReason(context: Context, partyId: Long, reason: String) {
        prefs(context).edit()
            .putString("last_skip_reason_supplier_party_$partyId", reason)
            .putLong("last_skip_reason_supplier_party_${partyId}_at", System.currentTimeMillis())
            .apply()
    }

    fun getLastSupplierPartySkipReason(context: Context, partyId: Long): String? =
        prefs(context).getString("last_skip_reason_supplier_party_$partyId", null)

    fun isSupplierPartyAutoPaused(context: Context, partyId: Long): Boolean {
        val until = prefs(context).getLong("pause_until_supplier_party_$partyId", 0L)
        return until > System.currentTimeMillis()
    }

    fun getSupplierPartyPauseUntil(context: Context, partyId: Long): Long {
        return prefs(context).getLong("pause_until_supplier_party_$partyId", 0L)
    }

    fun setSupplierPartyPauseUntil(context: Context, partyId: Long, untilMillis: Long) {
        prefs(context).edit().putLong("pause_until_supplier_party_$partyId", untilMillis).apply()
    }

    fun setManualPauseForSupplierParty(context: Context, partyId: Long, days: Int = 7) {
        val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
        setSupplierPartyPauseUntil(context, partyId, until)
    }

    fun markManualReminderSentSupplierParty(context: Context, partyId: Long, transactionId: Long) {
        val now = System.currentTimeMillis()
        prefs(context).edit()
            .putLong("manual_sent_supplier_party_${partyId}_tx_$transactionId", now)
            .putLong("manual_last_supplier_party_$partyId", now)
            .apply()
    }

    fun getLastManualReminderAtSupplierParty(context: Context, partyId: Long): Long {
        return prefs(context).getLong("manual_last_supplier_party_$partyId", 0L)
    }

    fun getReminderAttemptsSupplierParty(context: Context, partyId: Long): Int {
        return prefs(context).getInt("reminder_attempts_supplier_party_$partyId", 0)
    }

    fun incrementReminderAttemptsSupplierParty(context: Context, partyId: Long) {
        val key = "reminder_attempts_supplier_party_$partyId"
        val current = prefs(context).getInt(key, 0)
        prefs(context).edit().putInt(key, current + 1).apply()
    }

    fun getPreferredChannelSupplierParty(context: Context, partyId: Long): AutoReminderChannel? {
        val value = prefs(context).getString("preferred_channel_supplier_party_$partyId", null) ?: return null
        return runCatching { AutoReminderChannel.valueOf(value) }.getOrNull()
    }

    fun setPreferredChannelSupplierParty(context: Context, partyId: Long, channel: AutoReminderChannel) {
        prefs(context).edit().putString("preferred_channel_supplier_party_$partyId", channel.name).apply()
    }
}
