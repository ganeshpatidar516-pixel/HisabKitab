package com.ganesh.hisabkitabpro.commandos.adapters.hisabkitab

import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter

/**
 * Safe default adapter. It blocks all writes until explicit integration is enabled.
 * This prevents accidental regression in existing customer/ledger/bill flows.
 */
class SafeNoOpHisabKitabAdapter : DomainAdapter {
    override suspend fun searchCustomer(name: String): Boolean = false

    override suspend fun addCustomer(name: String, phone: String): Boolean = false

    override suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean = false

    override suspend fun clearBill(customerName: String): Boolean = false

    override suspend fun sendReminder(customerName: String): Boolean = false

    override suspend fun updateSetting(key: String, value: String): Boolean = false
}
