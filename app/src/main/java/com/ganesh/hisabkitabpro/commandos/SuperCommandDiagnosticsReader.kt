package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuperCommandDiagnosticsReader @Inject constructor(
    private val databaseLazy: dagger.Lazy<AppDatabase>
) {
    suspend fun recent(limit: Int = 10): List<AuditLogEntry> {
        return databaseLazy.get().auditLogDao().recentByEntityAndDetailToken(
            entityType = "SUPER_COMMAND",
            detailToken = "",
            limit = limit
        )
    }
}
