package com.ganesh.hisabkitabpro.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.ganesh.hisabkitabpro.data.repository.local.CustomerDao
import com.ganesh.hisabkitabpro.domain.customers.CustomerListMenuTab
import com.ganesh.hisabkitabpro.domain.customers.CustomerListReminderSegment
import com.ganesh.hisabkitabpro.domain.customers.CustomerListSortOption
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.sync.SyncEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🚀 HIGH-SCALE CUSTOMER REPOSITORY
 * Fixed freeze by using Lazy injection to defer DB overhead.
 */
class CustomerRepositoryImpl @Inject constructor(
    private val customerDaoLazy: dagger.Lazy<CustomerDao>,
    private val selectiveCloudMirror: SelectiveCloudMirror
) : CustomerRepository {

    private val customerDao get() = customerDaoLazy.get()

    override fun getAllCustomers(): Flow<List<Customer>> =
        customerDao.getAllCustomers().flowOn(Dispatchers.IO)

    override fun getCustomerCount(): Flow<Int> =
        customerDao.getCustomerCount().flowOn(Dispatchers.IO)

    override fun getOverallNetBalancePaise(): Flow<Long> =
        customerDao.getOverallNetBalancePaise().flowOn(Dispatchers.IO)

    override fun getDistinctCustomersRemindedCount(): Flow<Int> =
        customerDao.getDistinctCustomersRemindedCount().flowOn(Dispatchers.IO)

    override fun getDistinctRemindedCustomerIds(): Flow<List<Long>> =
        customerDao.getDistinctRemindedCustomerIds().flowOn(Dispatchers.IO)

    override fun getCustomerDueReminderCountNow(): Flow<Int> =
        customerDao.getCustomerDueReminderCountNow().flowOn(Dispatchers.IO)

    override fun getDueReminderCustomerIdsNow(): Flow<List<Long>> =
        customerDao.getDueReminderCustomerIdsNow().flowOn(Dispatchers.IO)

    override fun getRecentCustomers(): Flow<List<Customer>> =
        customerDao.getRecentCustomers().flowOn(Dispatchers.IO)

    override fun getCustomersPaged(
        query: String?,
        menuTab: CustomerListMenuTab,
        sortOption: CustomerListSortOption,
        reminderSegments: Set<CustomerListReminderSegment>
    ): Flow<PagingData<Customer>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = true),
            pagingSourceFactory = {
                customerDao.getCustomersPagingRaw(
                    buildCustomerPagingQuery(query, menuTab, sortOption, reminderSegments)
                )
            }
        ).flow.flowOn(Dispatchers.IO)
    }

    override suspend fun getCustomerById(customerId: Long): Customer? =
        customerDao.getCustomerById(customerId)

    override fun getCustomerByIdFlow(customerId: Long): Flow<Customer?> =
        customerDao.getCustomerByIdFlow(customerId).flowOn(Dispatchers.IO)

    override suspend fun addCustomer(customer: Customer) {
        val pendingCustomer = customer.copy(syncStatus = "PENDING")
        val insertedId = customerDao.insertCustomer(pendingCustomer)
        val persisted = pendingCustomer.copy(id = insertedId)
        SyncEngine.enqueueCustomer(persisted)
        selectiveCloudMirror.mirrorCustomer(persisted)
    }

    override suspend fun updateCustomer(customer: Customer) {
        val pendingCustomer = customer.copy(syncStatus = "PENDING", updatedAt = System.currentTimeMillis())
        customerDao.updateCustomer(pendingCustomer)
        SyncEngine.enqueueCustomer(pendingCustomer)
        selectiveCloudMirror.mirrorCustomer(pendingCustomer)
    }

    override suspend fun deleteCustomer(customerId: Long) {
        customerDao.softDeleteCustomer(customerId)
        customerDao.getCustomerByIdAny(customerId)?.let {
            SyncEngine.enqueueCustomer(it)
            selectiveCloudMirror.mirrorCustomer(it)
        }
    }

    override suspend fun searchCustomers(query: String): List<Customer> {
        return customerDao.searchCustomers(query)
    }

    override suspend fun updateCustomerBalance(customerId: Long, creditDelta: Long, debitDelta: Long) {
        val deltaPaise = creditDelta - debitDelta
        customerDao.updateBalanceCache(customerId, deltaPaise)
    }

    override suspend fun updateLastTransaction(customerId: Long, amount: Long, timestamp: Long) {
        customerDao.getCustomerById(customerId)?.let {
            customerDao.updateCustomer(it.copy(lastTransactionDate = timestamp, syncStatus = "PENDING"))
        }
    }

    override suspend fun updateRiskScore(customerId: Long, riskScore: Int) {
        customerDao.getCustomerById(customerId)?.let {
            customerDao.updateCustomer(it.copy(riskScore = riskScore, syncStatus = "PENDING"))
        }
    }

    override suspend fun incrementReminderCount(customerId: Long) {}
    override suspend fun resetReminderCount(customerId: Long) {}

    override suspend fun markReminderSent(customerId: Long) {
        val customer = customerDao.getCustomerById(customerId) ?: return
        customerDao.insertCustomerReminderSentLog(
            customerId = customerId,
            message = "Reminder sent to ${customer.name}"
        )
    }
}

private fun sanitizeSearch(raw: String?): String? {
    val t = raw?.trim()?.replace("%", "")?.replace("_", "") ?: return null
    return t.takeIf { it.isNotEmpty() }
}

private fun needsDefaulters(tab: CustomerListMenuTab, sort: CustomerListSortOption): Boolean =
    tab == CustomerListMenuTab.SORT_BY && sort == CustomerListSortOption.DEFAULTERS

/** Local calendar day match on millis `nextReminderDate` (SQLite device local time). */
private fun reminderDaySqlCondition(segment: CustomerListReminderSegment): String {
    val day = "date(nextReminderDate/1000, 'unixepoch', 'localtime')"
    val today = "date('now', 'localtime')"
    return when (segment) {
        CustomerListReminderSegment.TODAY ->
            "(nextReminderDate IS NOT NULL AND $day = $today)"
        CustomerListReminderSegment.PENDING ->
            "(nextReminderDate IS NOT NULL AND $day < $today)"
        CustomerListReminderSegment.UPCOMING ->
            "(nextReminderDate IS NOT NULL AND $day > $today)"
    }
}

private fun reminderFilterWhere(segments: Set<CustomerListReminderSegment>): String? {
    if (segments.isEmpty()) return null
    return segments.joinToString(prefix = "(", postfix = ")", separator = " OR ") { reminderDaySqlCondition(it) }
}

private fun orderBySort(tab: CustomerListMenuTab, sort: CustomerListSortOption): String {
    return when (tab) {
        CustomerListMenuTab.SORT_BY -> when (sort) {
            CustomerListSortOption.DEFAULT ->
                "ORDER BY name COLLATE NOCASE ASC"
            CustomerListSortOption.LAST_PAYMENT ->
                "ORDER BY CASE WHEN lastTransactionDate IS NULL THEN 1 ELSE 0 END, lastTransactionDate DESC, name COLLATE NOCASE ASC"
            CustomerListSortOption.LATEST_ACTIVITY ->
                "ORDER BY updatedAt DESC, name COLLATE NOCASE ASC"
            CustomerListSortOption.DUE_AMOUNT ->
                "ORDER BY balanceCache DESC, name COLLATE NOCASE ASC"
            CustomerListSortOption.NAME ->
                "ORDER BY name COLLATE NOCASE ASC"
            CustomerListSortOption.DEFAULTERS ->
                "ORDER BY balanceCache DESC, name COLLATE NOCASE ASC"
        }
        CustomerListMenuTab.REMINDER_DATE ->
            "ORDER BY CASE WHEN nextReminderDate IS NULL THEN 1 ELSE 0 END, nextReminderDate ASC, name COLLATE NOCASE ASC"
    }
}

private fun buildCustomerPagingQuery(
    query: String?,
    menuTab: CustomerListMenuTab,
    sortOption: CustomerListSortOption,
    reminderSegments: Set<CustomerListReminderSegment>
): SimpleSQLiteQuery {
    val sb = StringBuilder("SELECT * FROM customers WHERE isDeleted = 0")
    val args = mutableListOf<Any>()
    if (needsDefaulters(menuTab, sortOption)) {
        sb.append(" AND balanceCache > 0")
    }
    if (menuTab == CustomerListMenuTab.REMINDER_DATE) {
        reminderFilterWhere(reminderSegments)?.let { sb.append(" AND ").append(it) }
    }
    val sanitized = sanitizeSearch(query)
    if (sanitized != null) {
        sb.append(" AND (name LIKE '%' || ? || '%' OR phone LIKE '%' || ? || '%')")
        args.add(sanitized)
        args.add(sanitized)
    }
    sb.append(' ').append(orderBySort(menuTab, sortOption))
    return SimpleSQLiteQuery(sb.toString(), args.toTypedArray())
}
