package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.domain.sync.SyncItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncItemEntity): Long

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingItems(): Flow<List<SyncItemEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingItemsOnce(): List<SyncItemEntity>

    @Query("SELECT * FROM sync_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getProcessableItems(limit: Int = 200): List<SyncItemEntity>

    /**
     * Backoff-aware variant used by the hardened [com.ganesh.hisabkitabpro.domain.sync.SyncEngine].
     *
     * Selects PENDING items whose last update was earlier than [pendingCutoff],
     * or FAILED items whose last update was earlier than [failedCutoff] AND
     * whose retryCount is below [maxRetryForAuto]. Items above the auto-retry
     * cap stay parked until a user-initiated deep retry resets them.
     *
     * NO SCHEMA CHANGE — uses existing columns only.
     */
    @Query(
        "SELECT * FROM sync_queue " +
            "WHERE (status = 'PENDING' AND updatedAt <= :pendingCutoff) " +
            "   OR (status = 'FAILED' AND updatedAt <= :failedCutoff AND retryCount < :maxRetryForAuto) " +
            "ORDER BY updatedAt ASC " +
            "LIMIT :limit"
    )
    suspend fun getProcessableEligibleItems(
        pendingCutoff: Long,
        failedCutoff: Long,
        maxRetryForAuto: Int,
        limit: Int = 200
    ): List<SyncItemEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    suspend fun getPendingCountOnce(): Int

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'FAILED'")
    suspend fun getFailedCountOnce(): Int

    @Query("UPDATE sync_queue SET status = 'PENDING', retryCount = 0, updatedAt = :updatedAt WHERE status = 'FAILED'")
    suspend fun requeueFailedItems(updatedAt: Long): Int

    /**
     * Backdates every PENDING row's [updatedAt] so it becomes eligible for the
     * very next cycle. Used by the deep-retry path; does not change any
     * business data.
     */
    @Query("UPDATE sync_queue SET updatedAt = :updatedAt WHERE status = 'PENDING'")
    suspend fun touchAllPending(updatedAt: Long)

    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatusById(id: Long, status: String, retryCount: Int, updatedAt: Long)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Update
    suspend fun update(item: SyncItemEntity)

    @Delete
    suspend fun delete(item: SyncItemEntity)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSynced()
}
