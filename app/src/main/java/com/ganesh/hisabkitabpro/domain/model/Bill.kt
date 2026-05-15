package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * HISABKITAB PRO - FINAL MASTER STRUCTURE
 * 🧱 1. DATABASE - bills table
 * Optimized for High-Speed Audit-Proof Security with Composite Index.
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["customerId", "createdAt"]), // HIGH-SPEED RETRIEVAL INDEX
        Index(value = ["status"])
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val totalAmount: Long, // In Paise
    val status: String, // PENDING, PAID, CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)
