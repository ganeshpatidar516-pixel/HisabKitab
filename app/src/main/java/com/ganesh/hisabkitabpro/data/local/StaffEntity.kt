package com.ganesh.hisabkitabpro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Staff record. Lives in its own row-space — not joined to any customer/bill
 * tables, so no staff transaction can leak into the main business net balance.
 *
 * PII fields (`phoneEnc`, `emailEnc`) are persisted as Base64 AES-GCM
 * ciphertext sealed by [com.ganesh.hisabkitabpro.security.KeyStoreCryptoManager].
 * The legacy plaintext [phone] column is retained for backwards-compat lookups
 * and is gradually migrated by [StaffSecureFields] when records are read.
 *
 * Money is stored as **paise** (Long) — same convention as the rest of the app.
 */
@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val phone: String,
    val role: String = "STAFF",
    val permissions: String = "VIEW_ONLY",
    val businessId: String,
    val isDeleted: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,

    // Payroll core (added in DB v38)
    val designation: String = "",
    val salaryType: String = SALARY_TYPE_MONTHLY,
    val salaryAmountPaise: Long = 0L,
    val joiningDate: Long = 0L,
    val workdaysPerWeek: Int = 6,

    // Encrypted PII at-rest (added in DB v38, optional — null until first save)
    val phoneEnc: String? = null,
    val emailEnc: String? = null,

    // Free-form metadata
    val address: String = "",
    val notes: String = "",
    val photoUri: String = ""
) {
    companion object {
        const val SALARY_TYPE_MONTHLY = "MONTHLY"
        const val SALARY_TYPE_DAILY = "DAILY"
        const val SALARY_TYPE_WEEKLY = "WEEKLY"
    }
}
