package com.ganesh.hisabkitabpro.commandos.sync

enum class ConflictPolicy {
    SERVER_WINS,
    LOCAL_WINS,
    MANUAL_REVIEW
}

data class SyncConflict(
    val idempotencyKey: String,
    val localPayload: String,
    val serverPayload: String
)

sealed class ConflictResolution {
    data class UseLocal(val payload: String) : ConflictResolution()
    data class UseServer(val payload: String) : ConflictResolution()
    data class RequireManualReview(val reason: String) : ConflictResolution()
}

class ConflictResolver(
    private val policy: ConflictPolicy = ConflictPolicy.MANUAL_REVIEW
) {
    fun resolve(conflict: SyncConflict): ConflictResolution {
        return when (policy) {
            ConflictPolicy.SERVER_WINS -> ConflictResolution.UseServer(conflict.serverPayload)
            ConflictPolicy.LOCAL_WINS -> ConflictResolution.UseLocal(conflict.localPayload)
            ConflictPolicy.MANUAL_REVIEW -> ConflictResolution.RequireManualReview(
                reason = "Conflict detected for ${conflict.idempotencyKey}"
            )
        }
    }
}
