package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per discrete payroll event for a staff member: salary advance,
 * performance bonus, salary payment, deduction, etc.
 *
 * Strictly NOT linked to customer transactions / bills — payroll is a private
 * sub-ledger per staff. The merchant's main net balance is unaffected.
 */
@Entity(
    tableName = "staff_payroll_entry",
    indices = [
        Index(value = ["staffId", "dateMillis"]),
        Index(value = ["staffId", "kind", "dateMillis"])
    ]
)
data class StaffPayrollEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val staffId: String,
    val kind: String,
    val amountPaise: Long,
    val note: String = "",
    val dateMillis: Long,
    val cycleKey: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Int = 0
) {
    companion object {
        const val KIND_ADVANCE = "ADVANCE"
        const val KIND_BONUS = "BONUS"
        const val KIND_SALARY_PAID = "SALARY_PAID"
        const val KIND_DEDUCTION = "DEDUCTION"

        val ALL_KINDS: List<String> = listOf(
            KIND_ADVANCE,
            KIND_BONUS,
            KIND_SALARY_PAID,
            KIND_DEDUCTION
        )
    }
}
