package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * HISABKITAB PRO - ULTRA PRO SUPPLIER ARCHITECTURE
 * Optimized for Massive Scale (5 Crore Shield).
 */
@Entity(
    tableName = "suppliers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["phone"], unique = true),
        Index(value = ["isDeleted", "balanceCache"])
    ]
)
data class Supplier(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String? = null,
    val companyName: String? = null,
    val gstNumber: String? = null,
    
    // 🧱 SYNC & STATE
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING",
    
    // Logic Fields
    val balanceCache: Long = 0, // Total Payable (Paise)
    val lastTransactionDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
