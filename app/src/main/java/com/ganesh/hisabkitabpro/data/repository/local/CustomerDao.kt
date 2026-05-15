package com.ganesh.hisabkitabpro.data.repository.local

import androidx.paging.PagingSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import com.ganesh.hisabkitabpro.domain.model.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0")
    fun getCustomerCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(balanceCache), 0) FROM customers WHERE isDeleted = 0")
    fun getOverallNetBalancePaise(): Flow<Long>

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0 AND nextReminderDate IS NOT NULL")
    fun getReminderConfiguredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM customers WHERE isDeleted = 0 AND nextReminderDate IS NULL")
    fun getReminderNotConfiguredCount(): Flow<Int>

    @Query(
        "SELECT COUNT(DISTINCT customerId) FROM reminders " +
            "WHERE counterpartyKind = 'CUSTOMER' AND isSent = 1 AND customerId > 0"
    )
    fun getDistinctCustomersRemindedCount(): Flow<Int>

    @Query(
        "SELECT DISTINCT customerId FROM reminders " +
            "WHERE counterpartyKind = 'CUSTOMER' AND isSent = 1 AND customerId > 0"
    )
    fun getDistinctRemindedCustomerIds(): Flow<List<Long>>

    @Query(
        "SELECT COUNT(*) FROM reminders " +
            "WHERE counterpartyKind = 'CUSTOMER' AND isSent = 0 AND customerId > 0 " +
            "AND scheduledAt <= (strftime('%s','now') * 1000)"
    )
    fun getCustomerDueReminderCountNow(): Flow<Int>

    @Query(
        "SELECT DISTINCT customerId FROM reminders " +
            "WHERE counterpartyKind = 'CUSTOMER' AND isSent = 0 AND customerId > 0 " +
            "AND scheduledAt <= (strftime('%s','now') * 1000)"
    )
    fun getDueReminderCustomerIdsNow(): Flow<List<Long>>

    @Query(
        "INSERT INTO reminders (customerId, counterpartyKind, partyId, message, scheduledAt, isSent, priority, type, createdAt, lastEscalationTier) " +
            "VALUES (:customerId, 'CUSTOMER', 0, :message, :sentAt, 1, 'NORMAL', 'PAYMENT', :sentAt, 0)"
    )
    suspend fun insertCustomerReminderSentLog(customerId: Long, message: String, sentAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY updatedAt DESC LIMIT 50")
    fun getRecentCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getCustomersPaging(): PagingSource<Int, Customer>

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    suspend fun getCustomerById(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerByIdAny(id: Long): Customer?

    @Query("SELECT * FROM customers WHERE id = :id AND isDeleted = 0")
    fun getCustomerByIdFlow(id: Long): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Query("UPDATE customers SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteCustomer(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') AND isDeleted = 0")
    fun searchCustomersPaging(query: String): PagingSource<Int, Customer>

    @RawQuery(observedEntities = [Customer::class])
    fun getCustomersPagingRaw(query: SupportSQLiteQuery): PagingSource<Int, Customer>

    @Query("SELECT * FROM customers WHERE (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') AND isDeleted = 0")
    suspend fun searchCustomers(query: String): List<Customer>

    @Query("SELECT * FROM customers WHERE balanceCache > 0 AND isDeleted = 0")
    suspend fun getDebtors(): List<Customer>

    @Query("SELECT * FROM customers WHERE isDeleted = 0 AND id IN (:ids)")
    suspend fun getCustomersByIds(ids: List<Long>): List<Customer>

    @Query("SELECT id FROM customers WHERE isDeleted = 0")
    suspend fun getAllCustomerIds(): List<Long>

    @Query(
        "SELECT * FROM customers WHERE isDeleted = 0 AND balanceCache > 0 " +
            "ORDER BY balanceCache DESC LIMIT :limit"
    )
    suspend fun getTopDebtorsLimited(limit: Int): List<Customer>

    @Query(
        "SELECT name FROM customers WHERE isDeleted = 0 " +
            "ORDER BY name COLLATE NOCASE ASC LIMIT :limit"
    )
    suspend fun getCustomerNamesLimited(limit: Int): List<String>

    @Query(
        "SELECT id FROM customers WHERE isDeleted = 0 AND " +
            "REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '+', '') LIKE '%' || :digitSuffix"
    )
    suspend fun getCustomerIdsByPhoneDigitSuffix(digitSuffix: String): List<Long>

    @Query("UPDATE customers SET balanceCache = balanceCache + :delta, updatedAt = :timestamp WHERE id = :customerId")
    suspend fun updateBalanceCache(customerId: Long, delta: Long, timestamp: Long = System.currentTimeMillis())
}
