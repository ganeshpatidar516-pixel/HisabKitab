package com.ganesh.hisabkitabpro.addon.audit

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: AuditLogEntry): Long

    @Query("SELECT * FROM audit_log ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 200): List<AuditLogEntry>

    @Query(
        "SELECT * FROM audit_log " +
            "WHERE entityType = :entityType AND entityId = :entityId " +
            "ORDER BY createdAt DESC LIMIT :limit"
    )
    suspend fun recentByEntityId(
        entityType: String,
        entityId: Long,
        limit: Int = 200
    ): List<AuditLogEntry>

    @Query(
        "SELECT * FROM audit_log " +
            "WHERE entityType = :entityType AND detail LIKE '%' || :detailToken || '%' " +
            "ORDER BY createdAt DESC LIMIT :limit"
    )
    suspend fun recentByEntityAndDetailToken(
        entityType: String,
        detailToken: String,
        limit: Int = 200
    ): List<AuditLogEntry>
}
