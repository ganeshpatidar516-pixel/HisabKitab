package com.ganesh.hisabkitabpro.addon.audit

import com.ganesh.hisabkitabpro.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fire-and-forget audit writes — never blocks hot transaction paths.
 */
@Singleton
class AuditLogRecorder @Inject constructor(
    private val databaseLazy: dagger.Lazy<AppDatabase>
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun recordAsync(entityType: String, entityId: Long, action: String, detail: String? = null) {
        scope.launch {
            try {
                databaseLazy.get().auditLogDao().insert(
                    AuditLogEntry(
                        entityType = entityType,
                        entityId = entityId,
                        action = action,
                        detail = detail
                    )
                )
            } catch (_: Exception) {
                // Never break core flows
            }
        }
    }
}
