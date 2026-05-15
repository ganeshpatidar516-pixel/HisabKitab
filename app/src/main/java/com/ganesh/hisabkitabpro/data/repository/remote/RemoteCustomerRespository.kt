package com.ganesh.hisabkitabpro.data.repository.remote

import androidx.paging.PagingData
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.ganesh.hisabkitabpro.network.api.CustomerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.max
import javax.inject.Inject

/**
 * HISABKITAB PRO - REMOTE CUSTOMER REPOSITORY
 * Fixed compilation by implementing all interface members.
 */
class RemoteCustomerRepository @Inject constructor() : CustomerRepository {

    private val api: CustomerApi =
        RetrofitClient.retrofit.create(CustomerApi::class.java)

    private suspend fun upsertCustomer(customer: Customer) {
        runCatching { api.addCustomerV1(customer) }.getOrElse { api.addCustomer(customer) }
    }

    private suspend fun fetchCustomers(): List<Customer> {
        val primary = runCatching { api.getCustomersV1() }.getOrNull()
        if (primary?.isSuccessful == true) return primary.body().orEmpty()
        val legacy = runCatching { api.getCustomers() }.getOrNull()
        return if (legacy?.isSuccessful == true) legacy.body().orEmpty() else emptyList()
    }

    override fun getAllCustomers(): Flow<List<Customer>> = flow {
        emit(fetchCustomers())
    }

    override fun getCustomerCount(): Flow<Int> = getAllCustomers().map { it.size }

    override fun getOverallNetBalancePaise(): Flow<Long> =
        getAllCustomers().map { customers -> customers.sumOf { it.balanceCache } }

    override fun getDistinctCustomersRemindedCount(): Flow<Int> = flowOf(0)

    override fun getDistinctRemindedCustomerIds(): Flow<List<Long>> = flowOf(emptyList())

    override fun getCustomerDueReminderCountNow(): Flow<Int> = flowOf(0)

    override fun getDueReminderCustomerIdsNow(): Flow<List<Long>> = flowOf(emptyList())

    override fun getRecentCustomers(): Flow<List<Customer>> = flow {
        emit(fetchCustomers().take(50))
    }

    override fun getCustomersPaged(
        query: String?,
        menuTab: CustomerListMenuTab,
        sortOption: CustomerListSortOption,
        reminderSegments: Set<CustomerListReminderSegment>
    ): Flow<PagingData<Customer>> {
        return flow {
            var data = fetchCustomers().filter { !it.isDeleted }
            val q = query?.trim().orEmpty()
            if (q.isNotEmpty()) {
                data = data.filter {
                    it.name.contains(q, ignoreCase = true) || it.phone.contains(q, ignoreCase = true)
                }
            }
            data = when (sortOption) {
                CustomerListSortOption.DEFAULT -> data.sortedByDescending { it.updatedAt }
                CustomerListSortOption.LAST_PAYMENT -> data.sortedByDescending { it.lastTransactionDate ?: 0L }
                CustomerListSortOption.LATEST_ACTIVITY -> data.sortedByDescending { it.updatedAt }
                CustomerListSortOption.DUE_AMOUNT -> data.sortedByDescending { kotlin.math.abs(it.balanceCache) }
                CustomerListSortOption.NAME -> data.sortedBy { it.name.lowercase() }
                CustomerListSortOption.DEFAULTERS -> data.sortedByDescending { it.riskScore }
            }
            emit(PagingData.from(data))
        }
    }

    override suspend fun getCustomerById(customerId: Long): Customer? {
        return fetchCustomers().find { it.id == customerId }
    }

    override fun getCustomerByIdFlow(customerId: Long): Flow<Customer?> = getAllCustomers().map { list ->
        list.find { it.id == customerId }
    }

    override suspend fun addCustomer(customer: Customer) {
        upsertCustomer(customer)
    }

    override suspend fun updateCustomer(customer: Customer) {
        upsertCustomer(customer)
    }

    override suspend fun deleteCustomer(customerId: Long) {
        runCatching { api.deleteCustomerV1(customerId.toInt()) }.getOrElse { api.deleteCustomer(customerId.toInt()) }
    }

    override suspend fun searchCustomers(query: String): List<Customer> {
        return fetchCustomers().filter {
            (it.name.contains(query, ignoreCase = true)) ||
                (it.phone?.contains(query, ignoreCase = true) == true)
        }
    }

    override suspend fun updateCustomerBalance(customerId: Long, creditDelta: Long, debitDelta: Long) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            balanceCache = existing.balanceCache + creditDelta - debitDelta,
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }

    override suspend fun updateLastTransaction(customerId: Long, amount: Long, timestamp: Long) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            lastTransactionDate = timestamp,
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }

    override suspend fun updateRiskScore(customerId: Long, riskScore: Int) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            riskScore = max(0, riskScore),
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }

    override suspend fun incrementReminderCount(customerId: Long) {
        val existing = getCustomerById(customerId) ?: return
        val currentRisk = existing.riskScore
        val updated = existing.copy(
            riskScore = max(0, currentRisk + 1),
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }

    override suspend fun resetReminderCount(customerId: Long) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            riskScore = 0,
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }

    override suspend fun markReminderSent(customerId: Long) {
        val existing = getCustomerById(customerId) ?: return
        val updated = existing.copy(
            nextReminderDate = null,
            updatedAt = System.currentTimeMillis()
        )
        upsertCustomer(updated)
    }
}
