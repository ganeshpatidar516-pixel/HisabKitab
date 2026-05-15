package com.ganesh.hisabkitabpro.data.repository.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.Supplier
import kotlinx.coroutines.flow.Flow

/**
 * HISABKITAB PRO - SUPPLIER DAO (THE 5-CRORE ENGINE)
 * High-speed data engine for supplier management.
 */
@Dao
interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier): Long

    @Update
    suspend fun updateSupplier(supplier: Supplier)

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): Supplier?

    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllActiveSuppliers(): Flow<List<Supplier>>

    @Query("SELECT * FROM suppliers WHERE isDeleted = 0 ORDER BY name ASC")
    fun getSuppliersPagingSource(): PagingSource<Int, Supplier>

    @Query("SELECT * FROM suppliers WHERE name LIKE :query AND isDeleted = 0 ORDER BY name ASC")
    fun searchSuppliers(query: String): PagingSource<Int, Supplier>

    @Query("UPDATE suppliers SET isDeleted = 1, syncStatus = 'PENDING', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE suppliers SET balanceCache = :balance, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateBalanceCache(id: Long, balance: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT SUM(balanceCache) FROM suppliers WHERE isDeleted = 0")
    fun getGlobalTotalPayable(): Flow<Long?>
}
