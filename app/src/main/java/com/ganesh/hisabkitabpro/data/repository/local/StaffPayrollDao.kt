package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ganesh.hisabkitabpro.data.local.StaffPayrollEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffPayrollDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: StaffPayrollEntryEntity): Long

    @Update
    suspend fun update(entry: StaffPayrollEntryEntity)

    @Query(
        "UPDATE staff_payroll_entry SET isDeleted = 1, updatedAt = :timestamp WHERE id = :id"
    )
    suspend fun softDelete(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM staff_payroll_entry WHERE staffId = :staffId")
    suspend fun deleteForStaff(staffId: String)

    @Query(
        "SELECT * FROM staff_payroll_entry " +
            "WHERE staffId = :staffId AND isDeleted = 0 " +
            "ORDER BY dateMillis DESC, id DESC"
    )
    fun observeAllForStaff(staffId: String): Flow<List<StaffPayrollEntryEntity>>

    @Query(
        "SELECT * FROM staff_payroll_entry " +
            "WHERE staffId = :staffId AND isDeleted = 0 " +
            "  AND dateMillis BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY dateMillis ASC, id ASC"
    )
    suspend fun fetchRange(
        staffId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<StaffPayrollEntryEntity>

    /**
     * Live observation of payroll entries inside a closed time window —
     * powers the reactive Net Payable computation in the staff detail
     * screen. Same filter as [fetchRange], expressed as a Flow.
     */
    @Query(
        "SELECT * FROM staff_payroll_entry " +
            "WHERE staffId = :staffId AND isDeleted = 0 " +
            "  AND dateMillis BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY dateMillis ASC, id ASC"
    )
    fun observeRange(
        staffId: String,
        fromMillis: Long,
        toMillis: Long
    ): kotlinx.coroutines.flow.Flow<List<StaffPayrollEntryEntity>>

    @Query(
        "SELECT COALESCE(SUM(amountPaise), 0) FROM staff_payroll_entry " +
            "WHERE staffId = :staffId AND kind = :kind AND isDeleted = 0 " +
            "  AND dateMillis BETWEEN :fromMillis AND :toMillis"
    )
    suspend fun sumByKindInRange(
        staffId: String,
        kind: String,
        fromMillis: Long,
        toMillis: Long
    ): Long

    @Query(
        "SELECT COALESCE(SUM(amountPaise), 0) FROM staff_payroll_entry " +
            "WHERE staffId = :staffId AND kind = :kind AND isDeleted = 0"
    )
    suspend fun sumByKindAllTime(staffId: String, kind: String): Long
}
