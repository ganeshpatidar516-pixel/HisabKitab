package com.ganesh.hisabkitabpro.addon.audit

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Add-on audit trail — does not participate in ledger balance math. */
@Entity(
    tableName = "audit_log",
    indices = [Index(value = ["entityType", "entityId", "createdAt"])]
)
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val entityId: Long,
    val action: String,
    val detail: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
