package com.ganesh.hisabkitabpro.domain.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * HISABKITAB PRO - OFFLINE SYNC ENGINE
 * 🧱 6. OFFLINE SYNC ENGINE - Sync Queue
 */
@Entity(tableName = "sync_queue")
data class SyncItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // 'TRANSACTION', 'CUSTOMER', 'BILL'
    val payload: String, // JSON
    val status: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
