package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.*
import com.ganesh.hisabkitabpro.data.local.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun hardDeleteStaff(id: String)

    @Query("UPDATE staff SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteStaff(id: String, timestamp: Long = System.currentTimeMillis())

    /**
     * Reverse of [softDeleteStaff]. Restoring an archived staff record never
     * touches their financial history (attendance / payroll rows are kept
     * regardless of the parent's archive flag).
     */
    @Query("UPDATE staff SET isDeleted = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun restoreStaff(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM staff WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query(
        "SELECT * FROM staff " +
            "WHERE isDeleted = 0 AND (businessId = :businessId OR businessId = 'default_business') " +
            "ORDER BY name ASC"
    )
    fun getAllStaffForBusiness(businessId: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE isDeleted = 0 AND isActive = 1 ORDER BY name ASC")
    fun getActiveStaff(): Flow<List<StaffEntity>>

    /** Archived (soft-deleted) staff — used by the Archive viewer / restore UX. */
    @Query("SELECT * FROM staff WHERE isDeleted = 1 ORDER BY name ASC")
    fun getArchivedStaff(): Flow<List<StaffEntity>>

    @Query(
        "SELECT * FROM staff " +
            "WHERE isDeleted = 1 AND (businessId = :businessId OR businessId = 'default_business') " +
            "ORDER BY name ASC"
    )
    fun getArchivedStaffForBusiness(businessId: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff WHERE id = :id AND isDeleted = 0")
    suspend fun getStaffById(id: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getStaffByIdAny(id: String): StaffEntity?

    @Query("SELECT * FROM staff WHERE id = :id AND isDeleted = 0")
    fun observeStaffById(id: String): Flow<StaffEntity?>

    /** Observe a staff record regardless of archive state — for audit / restore views. */
    @Query("SELECT * FROM staff WHERE id = :id")
    fun observeStaffByIdAny(id: String): Flow<StaffEntity?>

    @Query(
        "SELECT * FROM staff " +
            "WHERE id = :id AND (businessId = :businessId OR businessId = 'default_business')"
    )
    fun observeStaffByIdAnyForBusiness(id: String, businessId: String): Flow<StaffEntity?>

    @Query("SELECT COUNT(*) FROM staff WHERE isDeleted = 0")
    suspend fun countActive(): Int
}
