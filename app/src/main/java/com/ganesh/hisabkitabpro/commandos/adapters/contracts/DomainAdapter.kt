package com.ganesh.hisabkitabpro.commandos.adapters.contracts

data class ReminderDispatchReport(
    val success: Boolean,
    val customerName: String,
    val channel: String? = null,
    val reason: String? = null
)

interface DomainAdapter {
    suspend fun searchCustomer(name: String): Boolean
    suspend fun addCustomer(name: String, phone: String): Boolean
    suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean
    suspend fun clearBill(customerName: String): Boolean
    suspend fun sendReminder(customerName: String): Boolean
    suspend fun updateSetting(key: String, value: String): Boolean

    suspend fun sendReminderWithReport(customerName: String): ReminderDispatchReport {
        val ok = sendReminder(customerName)
        return if (ok) {
            ReminderDispatchReport(success = true, customerName = customerName, channel = "unknown")
        } else {
            ReminderDispatchReport(success = false, customerName = customerName, reason = "dispatch_failed")
        }
    }

    /** Fuzzy name hints from live customer list (empty = no integration / no data). */
    suspend fun suggestCustomerNames(query: String, limit: Int): List<String> = emptyList()

    /**
     * Answer a read-only business question from local DB. Return null to defer to other handlers.
     * [customerHint] is set when the parser already extracted a probable customer name.
     */
    suspend fun answerLedgerInsightQuery(normalizedInput: String, customerHint: String?): String? = null

    /**
     * Optional inventory bridge. Implementations may answer read-only inventory
     * questions or execute local-only product/stock commands. Return null when
     * the input is not inventory-related.
     */
    suspend fun handleInventoryCommand(normalizedInput: String): String? = null
}
