package com.ganesh.hisabkitabpro.data.repository.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.SupplierTransaction
import kotlinx.coroutines.flow.Flow

/**
 * HISABKITAB PRO - SUPPLIER TRANSACTION DAO
 */
@Dao
interface SupplierTransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: SupplierTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: SupplierTransaction)

    @Query("SELECT * FROM supplier_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): SupplierTransaction?

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsBySupplierId(supplierId: Long): Flow<List<SupplierTransaction>>

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getTransactionsPagingSource(supplierId: Long): PagingSource<Int, SupplierTransaction>

    @Query("UPDATE supplier_transactions SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("""
        SELECT 
        COALESCE(SUM(CASE WHEN type = 'PURCHASE' THEN amount END), 0) -
        COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount END), 0)
        FROM supplier_transactions
        WHERE supplierId = :supplierId 
        AND isDeleted = 0
    """)
    suspend fun calculatePayable(supplierId: Long): Long
}
