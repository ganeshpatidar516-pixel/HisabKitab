package com.ganesh.hisabkitabpro.data.repository.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ganesh.hisabkitabpro.data.local.StaffAttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffAttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: StaffAttendanceEntity): Long

    @Update
    suspend fun update(record: StaffAttendanceEntity)

    @Query("DELETE FROM staff_attendance WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId AND dateMillis = :dateMillis")
    suspend fun deleteForDay(staffId: String, dateMillis: Long)

    @Query("DELETE FROM staff_attendance WHERE staffId = :staffId")
    suspend fun deleteForStaff(staffId: String)

    @Query(
        "SELECT * FROM staff_attendance WHERE staffId = :staffId AND dateMillis = :dateMillis LIMIT 1"
    )
    suspend fun findForDay(staffId: String, dateMillis: Long): StaffAttendanceEntity?

    @Query(
        "SELECT * FROM staff_attendance " +
            "WHERE staffId = :staffId AND dateMillis BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY dateMillis ASC"
    )
    fun observeRange(
        staffId: String,
        fromMillis: Long,
        toMillis: Long
    ): Flow<List<StaffAttendanceEntity>>

    @Query(
        "SELECT * FROM staff_attendance " +
            "WHERE staffId = :staffId AND dateMillis BETWEEN :fromMillis AND :toMillis " +
            "ORDER BY dateMillis ASC"
    )
    suspend fun fetchRange(
        staffId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<StaffAttendanceEntity>

    @Query(
        "SELECT status, COUNT(*) AS count FROM staff_attendance " +
            "WHERE staffId = :staffId AND dateMillis BETWEEN :fromMillis AND :toMillis " +
            "GROUP BY status"
    )
    suspend fun statusCounts(
        staffId: String,
        fromMillis: Long,
        toMillis: Long
    ): List<AttendanceStatusCount>
}

data class AttendanceStatusCount(
    val status: String,
    val count: Int
)
