package com.ganesh.hisabkitabpro.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * HISABKITAB PRO - FINAL MASTER STRUCTURE
 * 🧱 1. DATABASE - transaction_history table
 * Optimized for Audit Trails with Foreign Key Integrity.
 */
@Entity(
    tableName = "transaction_history",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"])
    ]
)
data class TransactionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val oldAmount: Long,
    val newAmount: Long,
    val editedAt: Long = System.currentTimeMillis()
)
