package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per (staff, business-day) — uniqueness enforced by index.
 *
 * Lives entirely outside the customer / supplier ledger; cannot affect any
 * net-balance computation in the existing accounting flows.
 *
 * `dateMillis` is the *start of the local business day* in epoch millis. The
 * day-key normalization is the caller's responsibility — see
 * [com.ganesh.hisabkitabpro.domain.payroll.AttendanceDayKey].
 */
@Entity(
    tableName = "staff_attendance",
    indices = [
        Index(value = ["staffId", "dateMillis"], unique = true),
        Index(value = ["dateMillis"])
    ]
)
data class StaffAttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: String,
    val dateMillis: Long,
    val status: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PRESENT = "PRESENT"
        const val STATUS_ABSENT = "ABSENT"
        const val STATUS_HALF_DAY = "HALF_DAY"
        const val STATUS_LEAVE = "LEAVE"

        val ALL_STATUSES: List<String> = listOf(
            STATUS_PRESENT,
            STATUS_ABSENT,
            STATUS_HALF_DAY,
            STATUS_LEAVE
        )
    }
}
