package com.ganesh.hisabkitabpro.data.repository.local

import androidx.paging.PagingSource
import androidx.room.*
import com.ganesh.hisabkitabpro.domain.model.Party
import kotlinx.coroutines.flow.Flow

/**
 * HISABKITAB PRO ULTRA - PARTY DAO
 * Unified engine for Customers and Suppliers.
 */
@Dao
interface PartyDao {

    /**
     * P0 — global supplier payable (paise) from [parties]; single source of truth with supplier ledger.
     * Negative net is clamped in [PartyRepositoryImpl] to match list/dashboard semantics.
     */
    @Query(
        "SELECT COALESCE(SUM(totalBalance), 0) FROM parties WHERE isSupplier = 1 AND isDeleted = 0"
    )
    fun observeActiveSuppliersTotalBalancePaise(): Flow<Long>

    @Query("SELECT * FROM parties WHERE isSupplier = :isSupplier AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllParties(isSupplier: Boolean): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE isSupplier = :isSupplier AND isDeleted = 0 ORDER BY name ASC")
    fun getPartiesPagingSource(isSupplier: Boolean): PagingSource<Int, Party>

    @Query(
        "SELECT * FROM parties WHERE isSupplier = :isSupplier AND isDeleted = 0 AND " +
            "(name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR " +
            "IFNULL(city, '') LIKE '%' || :query || '%') ORDER BY name ASC"
    )
    fun searchPartiesPagingSource(isSupplier: Boolean, query: String): PagingSource<Int, Party>

    /** One-shot backfill: suppliers with no city in DB but city stored in [SupplierProfilePrefs]. */
    @Query(
        "SELECT * FROM parties WHERE isSupplier = 1 AND isDeleted = 0 AND " +
            "(city IS NULL OR TRIM(city) = '')"
    )
    suspend fun getActiveSuppliersWithEmptyCityColumn(): List<Party>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParty(party: Party): Long

    @Update
    suspend fun updateParty(party: Party)

    @Query("UPDATE parties SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteParty(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM parties WHERE id = :id LIMIT 1")
    suspend fun getPartyById(id: Long): Party?

    @Query("UPDATE parties SET totalBalance = :balance, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateBalance(id: Long, balance: Long, timestamp: Long = System.currentTimeMillis())
}
