package com.ganesh.hisabkitabpro.domain.sync

/**
 * Result classification for a single item's sync attempt and for a full sync
 * cycle. Used by [SyncEngine] to talk to [SyncHealthMonitor] without leaking
 * Retrofit / OkHttp / Drive types.
 */

/** What kind of failure happened — drives whether/when we retry. */
enum class SyncFailureKind(val transient: Boolean, val display: String) {
    /** Connectivity, DNS, TLS handshake, socket timeout. Transient. */
    Network(transient = true, display = "Network unavailable"),

    /** 401 / 403 — token missing/expired. Don't burn retries on this. */
    AuthExpired(transient = false, display = "Session expired — please sign in again"),

    /** 429 — Google Drive / backend rate limit. Backoff harder. */
    Quota(transient = true, display = "Quota / rate-limited"),

    /** 408 / 5xx — backend hiccup. Transient. */
    ServerError(transient = true, display = "Server error"),

    /** 409 / 412 / 422 — server has a newer version. Resolve via LWW. */
    Conflict(transient = false, display = "Server has a newer copy — review in Cloud sync"),

    /** 4xx other than the above — typically permanent (bad payload). */
    ClientError(transient = false, display = "Bad payload"),

    /** JSON / serialization failures. Permanent. */
    Parse(transient = false, display = "Payload decode failed"),

    /** Anything we couldn't classify. Treated as transient. */
    Unknown(transient = true, display = "Unknown error");
}

/** Per-item outcome. */
sealed interface SyncItemOutcome {
    data class Success(val itemId: Long, val type: String) : SyncItemOutcome

    /** Server is newer; we converged via Last-Write-Wins by dropping the local row. */
    data class ResolvedByLww(val itemId: Long, val type: String) : SyncItemOutcome

    data class Failure(
        val itemId: Long,
        val type: String,
        val kind: SyncFailureKind,
        val rawReason: String
    ) : SyncItemOutcome
}

/** Aggregate cycle outcome surfaced to UI / [SyncHealthMonitor]. */
data class SyncCycleReport(
    val attempted: Int,
    val succeeded: Int,
    val resolvedByLww: Int,
    val transientFailures: Int,
    val permanentFailures: Int,
    val authExpired: Boolean,
    val noNetwork: Boolean,
    val durationMs: Long,
    val finishedAt: Long
) {
    val effectivelySucceeded: Int get() = succeeded + resolvedByLww
    val isFullyClean: Boolean get() = transientFailures == 0 &&
        permanentFailures == 0 && !authExpired && !noNetwork
}
