package com.ganesh.hisabkitabpro.data.repository.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction as RoomTransaction
import com.ganesh.hisabkitabpro.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun getTransactionByIdFlow(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE uniqueHash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): Transaction?

    @Query("""
        SELECT 
        COALESCE(SUM(CASE WHEN type IN ('CREDIT', 'INVOICE') THEN amount END), 0) -
        COALESCE(SUM(CASE WHEN type IN ('DEBIT', 'PAYMENT') THEN amount END), 0)
        FROM transactions
        WHERE customerId = :customerId 
        AND isDeleted = 0 
        AND status = 'SUCCESS'
    """)
    suspend fun calculateBalance(customerId: Long): Long

    @Query("SELECT SUM(amount) FROM transactions WHERE type IN ('CREDIT', 'INVOICE') AND isDeleted = 0 AND status = 'SUCCESS'")
    fun getGlobalTotalGiven(): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type IN ('DEBIT', 'PAYMENT') AND isDeleted = 0 AND status = 'SUCCESS'")
    fun getGlobalTotalReceived(): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY createdAt DESC LIMIT 50")
    fun getRecentActiveTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllTransactionsPaging(): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsByCustomerId(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsPagingSource(customerId: Long): PagingSource<Int, Transaction>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND isDeleted = 0 ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getTransactionsPaged(customerId: Long, limit: Int, offset: Int): List<Transaction>

    @Query("UPDATE transactions SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE customers SET balanceCache = :balance, updatedAt = :timestamp WHERE id = :customerId")
    suspend fun updateCustomerBalanceCache(customerId: Long, balance: Long, timestamp: Long = System.currentTimeMillis())

    @RoomTransaction
    suspend fun insertTransactionWithBalanceUpdate(transaction: Transaction): Long {
        val existing = getTransactionByHash(transaction.uniqueHash)
        if (existing != null) return existing.id
        val id = insertTransaction(transaction)
        val newBalance = calculateBalance(transaction.customerId)
        updateCustomerBalanceCache(transaction.customerId, newBalance)
        return id
    }

    @RoomTransaction
    suspend fun updateTransactionWithBalanceUpdate(transaction: Transaction) {
        updateTransaction(transaction)
        val newBalance = calculateBalance(transaction.customerId)
        updateCustomerBalanceCache(transaction.customerId, newBalance)
    }

    @RoomTransaction
    suspend fun softDeleteWithBalanceUpdate(id: Long) {
        val tx = getTransactionById(id) ?: return
        softDelete(id)
        val newBalance = calculateBalance(tx.customerId)
        updateCustomerBalanceCache(tx.customerId, newBalance)
    }

    @Query("UPDATE transactions SET isDeleted = 0, syncStatus = 'PENDING', updatedAt = :timestamp WHERE id = :id")
    suspend fun restore(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE transactions SET whatsappSentAt = :timestamp, syncStatus = 'PENDING', updatedAt = :timestamp
        WHERE id = :id AND isDeleted = 0
        """
    )
    suspend fun markWhatsAppBillSent(id: Long, timestamp: Long = System.currentTimeMillis())

    @RoomTransaction
    suspend fun restoreWithBalanceUpdate(id: Long) {
        val tx = getTransactionById(id) ?: return
        restore(id)
        val newBalance = calculateBalance(tx.customerId)
        updateCustomerBalanceCache(tx.customerId, newBalance)
    }
}
