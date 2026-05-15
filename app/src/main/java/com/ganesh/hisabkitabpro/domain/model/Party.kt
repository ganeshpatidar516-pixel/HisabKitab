package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * HISABKITAB PRO ULTRA - PARTY ENTITY (Unified Customer/Supplier)
 * Optimized for Massive Scale (5 Crore Shield) with Bulletproof Indexing.
 */
@Entity(
    tableName = "parties",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isSupplier"]),
        Index(value = ["createdAt"]),
        Index(value = ["isDeleted", "isSupplier"]) // Quick retrieval for list tabs
    ]
)
data class Party(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    /** Supplier city / locality; nullable for customers and legacy rows (prefs backfill). */
    val city: String? = null,
    val isSupplier: Boolean = false, // True = सप्लायर, False = कस्टमर
    val totalBalance: Long = 0, // In Paise (Blueprint Rule 9)
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
