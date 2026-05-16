package com.ganesh.hisabkitabpro.domain.repository

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption
import com.ganesh.hisabkitabpro.domain.model.Customer
import kotlinx.coroutines.flow.Flow

/** Max customers loaded for AI / assistant snapshots (P1 scale cap). */
const val CUSTOMER_AI_SNAPSHOT_LIMIT = 500

interface CustomerRepository {
    @Deprecated(
        message = "Prefer getCustomersPaged or searchCustomers; full-table Flow does not scale.",
        level = DeprecationLevel.WARNING,
    )
    fun getAllCustomers(): Flow<List<Customer>>

    suspend fun getCustomersByIds(ids: List<Long>): List<Customer>

    suspend fun getAllCustomerIds(): List<Long>

    suspend fun getTopDebtorsLimited(limit: Int): List<Customer>

    suspend fun getCustomerNamesLimited(limit: Int): List<String>

    suspend fun getCustomerIdsByPhoneDigitSuffix(digitSuffix: String): List<Long>

    suspend fun getDebtors(): List<Customer>
    fun getCustomerCount(): Flow<Int>
    fun getOverallNetBalancePaise(): Flow<Long>
    fun getDistinctCustomersRemindedCount(): Flow<Int>
    fun getDistinctRemindedCustomerIds(): Flow<List<Long>>
    fun getCustomerDueReminderCountNow(): Flow<Int>
    fun getDueReminderCustomerIdsNow(): Flow<List<Long>>
    fun getRecentCustomers(): Flow<List<Customer>>
    fun getCustomersPaged(
        query: String? = null,
        menuTab: CustomerListMenuTab = CustomerListMenuTab.SORT_BY,
        sortOption: CustomerListSortOption = CustomerListSortOption.DEFAULT,
        reminderSegments: Set<CustomerListReminderSegment> = emptySet()
    ): Flow<PagingData<Customer>>
    suspend fun getCustomerById(customerId: Long): Customer?
    fun getCustomerByIdFlow(customerId: Long): Flow<Customer?>
    suspend fun addCustomer(customer: Customer)
    suspend fun updateCustomer(customer: Customer)
    suspend fun deleteCustomer(customerId: Long)
    suspend fun searchCustomers(query: String): List<Customer>
    suspend fun updateCustomerBalance(customerId: Long, creditDelta: Long, debitDelta: Long)
    suspend fun updateLastTransaction(customerId: Long, amount: Long, timestamp: Long)
    suspend fun updateRiskScore(customerId: Long, riskScore: Int)
    suspend fun incrementReminderCount(customerId: Long)
    suspend fun resetReminderCount(customerId: Long)
    suspend fun markReminderSent(customerId: Long)
}
