package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ganesh.hisabkitabpro.data.repository.converters.StringListConverter

/**
 * HISABKITAB PRO - ULTRA SCALE CUSTOMER ARCHITECTURE
 * Optimized for Million-Row Datasets with High-Speed Composite Indexing.
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"], unique = true),
        Index(value = ["isDeleted", "balanceCache"]), // PERFORMANCE: Quick retrieval of active debtors
        Index(value = ["syncStatus"]),
        Index(value = ["updatedAt"])
    ]
)
@TypeConverters(StringListConverter::class)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String, // UNIQUE
    val address: String? = null,
    val nextReminderDate: Long? = null,
    
    // 🧱 GHOST RECORDS & SYNC PROTOCOL
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING", // PENDING, SYNCED
    
    // Additional professional fields
    val profileImage: String? = null,
    val creditLimit: Long = 0, // In Paise
    val isBlocked: Boolean = false,
    val reminderEnabled: Boolean = true,
    val balanceCache: Long = 0, // Cache for fast UI
    val riskScore: Int = 0,
    val tags: List<String>? = emptyList(),
    val lastTransactionDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
