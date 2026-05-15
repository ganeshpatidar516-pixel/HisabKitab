package com.ganesh.hisabkitabpro.domain.sync

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.di.SyncCloudMirrorEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.ganesh.hisabkitabpro.data.repository.local.SyncDao
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.network.api.CustomerApi
import com.ganesh.hisabkitabpro.network.api.TransactionApi
import com.ganesh.hisabkitabpro.network.api.toSyncRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * HARDENED SYNC ENGINE
 *
 * Public API is preserved (`enqueueTransaction`, `enqueueCustomer`,
 * `performFullSync`, `getHealthSnapshot`, `getPendingCount`,
 * `requeueFailedItems`, `initialize`) — every existing caller compiles and
 * behaves the same on the surface.
 *
 * Internal upgrades:
 *  - **Exponential backoff** via [SyncBackoffPolicy]. An item is only re-tried
 *    once `updatedAt + delay(retryCount)` has elapsed, so we no longer hammer
 *    the same row 5× in a single cycle.
 *  - **Auto-retry cap** ([SyncBackoffPolicy.MAX_AUTO_RETRIES]). Items above
 *    the cap stay parked as `FAILED` until the UI's deep-retry resets them,
 *    so a poisoned payload can't burn battery indefinitely.
 *  - **Atomic per-item commit**. `dao.deleteById` is wrapped in `runCatching`
 *    + status update so a partial commit can never leave a row PENDING after
 *    a successful upload (which would have caused server duplicates).
 *  - **Conflict handling (P2)** for HTTP 409 / 412 / 422: rows are marked
 *    `FAILED` with [SyncFailureKind.Conflict] for user deep-retry — local
 *    changes are not silently dropped.
 *  - **Failure classification** ([SyncFailureKind]) so the UI can distinguish
 *    Network vs AuthExpired vs Quota etc. and display correct copy.
 *  - **Sync Health Monitor signaling**: emits cycle start, per-item, and
 *    cycle-complete events so [SyncHealthMonitor.state] always reflects truth.
 *
 * No DB schema changes, no Customer/Bill/Ledger logic touched.
 */
object SyncEngine {

    private const val TAG = "SyncEngine"

    /**
     * Max items pulled per cycle. Kept at 200 to match prior behavior.
     */
    private const val BATCH_SIZE = 200

    /**
     * Soft cap retained for backwards-compat with the old call sites that
     * read `MAX_RETRY` from this object. The real auto-retry budget is in
     * [SyncBackoffPolicy.MAX_AUTO_RETRIES].
     */
    @Suppress("unused")
    private const val MAX_RETRY = SyncBackoffPolicy.MAX_AUTO_RETRIES

    private val queue = mutableListOf<SyncItem>()
    private val queueMutex = Mutex()
    private val gson = Gson()

    @Volatile
    private var syncDao: SyncDao? = null

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var lastAttemptAt: Long? = null

    @Volatile
    private var lastFailureReason: String? = null

    /** Backwards-compat snapshot type (kept for old callers). */
    data class SyncHealthSnapshot(
        val pendingInDb: Int,
        val failedInDb: Int,
        val queuedInMemory: Int,
        val lastAttemptAt: Long?,
        val lastFailureReason: String?
    ) {
        val totalOutstanding: Int get() = pendingInDb + failedInDb + queuedInMemory
    }

    fun initialize(syncDao: SyncDao, context: Context) {
        this.syncDao = syncDao
        this.appContext = context.applicationContext
        SyncHealthMonitor.initialize(syncDao)
    }

    /** P2 — reload RAM queue from disk after cold start, then flush into Room when possible. */
    suspend fun restorePersistedFallbackQueue() {
        val ctx = appContext ?: return
        val restored = SyncFallbackQueueStore.load(ctx)
        if (restored.isEmpty()) return
        queueMutex.withLock { queue.addAll(restored) }
        SyncHealthMonitor.onInMemoryQueueChanged(currentInMemoryQueueSize())
        Log.i(TAG, "Restored ${restored.size} sync item(s) from fallback store")
        flushInMemoryQueueToDb()
        persistFallbackQueueSnapshot()
    }

    /** Enqueue a transaction for upload. */
    suspend fun enqueueTransaction(transaction: Transaction) {
        val payload = gson.toJson(transaction)
        val item = SyncItem(
            id = transaction.id.toString(),
            type = "TRANSACTION",
            payload = payload,
            status = SyncStatus.PENDING
        )
        persistOrQueue(item)
    }

    /** Enqueue a customer for upload. */
    suspend fun enqueueCustomer(customer: Customer) {
        val payload = gson.toJson(customer)
        val item = SyncItem(
            id = customer.id.toString(),
            type = "CUSTOMER",
            payload = payload,
            status = SyncStatus.PENDING
        )
        persistOrQueue(item)
    }

    /**
     * Drives one full sync cycle: drains in-memory queue → selects eligible
     * rows from the DB (gated by exponential backoff) → uploads them → updates
     * status atomically. Always emits a [SyncCycleReport] to
     * [SyncHealthMonitor].
     */
    suspend fun performFullSync(
        customerApi: CustomerApi,
        transactionApi: TransactionApi
    ) = withContext(Dispatchers.IO) {
        SyncHealthMonitor.onCycleStart()

        val started = System.currentTimeMillis()
        lastAttemptAt = started

        val dao = syncDao
        if (dao == null) {
            lastFailureReason = "Sync DAO unavailable"
            Log.w(TAG, "Sync DAO is unavailable. Skipping sync cycle safely.")
            SyncHealthMonitor.onCycleComplete(
                SyncCycleReport(
                    attempted = 0,
                    succeeded = 0,
                    resolvedByLww = 0,
                    transientFailures = 0,
                    permanentFailures = 0,
                    authExpired = false,
                    noNetwork = false,
                    durationMs = System.currentTimeMillis() - started,
                    finishedAt = System.currentTimeMillis()
                )
            )
            return@withContext
        }

        flushInMemoryQueueToDb()
        SyncHealthMonitor.onInMemoryQueueChanged(currentInMemoryQueueSize())

        val now = System.currentTimeMillis()
        val pending = dao.getProcessableEligibleItems(
            pendingCutoff = now,
            // For FAILED rows: only items whose last attempt was > base*2^retry ago.
            // We pass `now - MIN_FAILED_GAP_MS` so the DB pre-filters by a coarse
            // gap; the fine-grained per-row check is done in [isEligible] below.
            failedCutoff = now - SyncBackoffPolicy.BASE_DELAY_MS,
            maxRetryForAuto = SyncBackoffPolicy.MAX_AUTO_RETRIES,
            limit = BATCH_SIZE
        )

        var succeeded = 0
        var lwwResolved = 0
        var transient = 0
        var permanent = 0
        var authExpired = false
        var noNetwork = false
        var attempted = 0

        for (entity in pending) {
            // Per-row backoff gate (exact, with the same retryCount we'll use).
            if (!SyncBackoffPolicy.isEligible(entity.retryCount, entity.updatedAt, now)) {
                continue
            }
            attempted++

            val newRetry = entity.retryCount + 1
            val outcome: SyncItemOutcome = try {
                when (entity.type) {
                    "CUSTOMER" -> uploadCustomer(entity, customerApi)
                    "TRANSACTION" -> uploadTransaction(entity, transactionApi)
                    else -> SyncItemOutcome.Failure(
                        itemId = entity.id,
                        type = entity.type,
                        kind = SyncFailureKind.ClientError,
                        rawReason = "unknown item type: ${entity.type}"
                    )
                }
            } catch (t: Throwable) {
                SyncItemOutcome.Failure(
                    itemId = entity.id,
                    type = entity.type,
                    kind = classify(t),
                    rawReason = t.message.orEmpty().take(180)
                )
            }

            SyncHealthMonitor.onItemOutcome(outcome)

            when (outcome) {
                is SyncItemOutcome.Success -> {
                    if (entity.type == "TRANSACTION") {
                        mirrorTransactionAfterFastApiSuccess(entity.payload)
                    } else if (entity.type == "CUSTOMER") {
                        mirrorCustomerAfterFastApiSuccess(entity.payload)
                    }
                    commitDelete(dao, entity, reasonOnFail = "post-success cleanup")
                    succeeded++
                    lastFailureReason = null
                }
                is SyncItemOutcome.ResolvedByLww -> {
                    commitDelete(dao, entity, reasonOnFail = "post-lww cleanup")
                    lwwResolved++
                }
                is SyncItemOutcome.Failure -> {
                    if (outcome.kind == SyncFailureKind.AuthExpired) authExpired = true
                    if (outcome.kind == SyncFailureKind.Network) noNetwork = true
                    if (outcome.kind.transient) transient++ else permanent++
                    val finalStatus =
                        if (SyncBackoffPolicy.hasExhaustedAutoRetries(newRetry) ||
                            !outcome.kind.transient
                        ) SyncStatus.FAILED.name
                        else SyncStatus.PENDING.name
                    runCatching {
                        dao.updateStatusById(
                            id = entity.id,
                            status = finalStatus,
                            retryCount = newRetry,
                            updatedAt = System.currentTimeMillis()
                        )
                    }.onFailure { Log.w(TAG, "Could not persist failure state", it) }
                    lastFailureReason = "${outcome.kind.display}: ${outcome.rawReason}"
                }
            }
        }

        val report = SyncCycleReport(
            attempted = attempted,
            succeeded = succeeded,
            resolvedByLww = lwwResolved,
            transientFailures = transient,
            permanentFailures = permanent,
            authExpired = authExpired,
            noNetwork = noNetwork,
            durationMs = System.currentTimeMillis() - started,
            finishedAt = System.currentTimeMillis()
        )
        SyncHealthMonitor.onCycleComplete(report)
        if (succeeded > 0) {
            runCatching { dao.clearSynced() }
                .onFailure { Log.w(TAG, "clearSynced housekeeping failed", it) }
        }
    }

    /** P2 — idempotent Firestore refresh after FastAPI confirms the row. */
    private fun mirrorTransactionAfterFastApiSuccess(payload: String) {
        val ctx = appContext ?: return
        val txn = runCatching { gson.fromJson(payload, Transaction::class.java) }.getOrNull() ?: return
        runCatching {
            EntryPointAccessors.fromApplication(ctx, SyncCloudMirrorEntryPoint::class.java)
                .selectiveCloudMirror()
                .mirrorTransaction(txn)
        }.onFailure { Log.w(TAG, "Post-upload transaction mirror failed", it) }
    }

    private fun mirrorCustomerAfterFastApiSuccess(payload: String) {
        val ctx = appContext ?: return
        val customer = runCatching { gson.fromJson(payload, Customer::class.java) }.getOrNull() ?: return
        runCatching {
            EntryPointAccessors.fromApplication(ctx, SyncCloudMirrorEntryPoint::class.java)
                .selectiveCloudMirror()
                .mirrorCustomer(customer)
        }.onFailure { Log.w(TAG, "Post-upload customer mirror failed", it) }
    }

    /* ---------------- Per-type uploaders ---------------- */

    private suspend fun uploadCustomer(
        entity: SyncItemEntity,
        api: CustomerApi
    ): SyncItemOutcome {
        val customer = runCatching { gson.fromJson(entity.payload, Customer::class.java) }
            .getOrElse {
                return SyncItemOutcome.Failure(
                    entity.id, entity.type, SyncFailureKind.Parse, it.message.orEmpty()
                )
            }
        val response = runCatching { api.addCustomerV1(customer) }
            .getOrElse { runCatching { api.addCustomer(customer) }.getOrThrow() }
        return interpret(entity, response)
    }

    private suspend fun uploadTransaction(
        entity: SyncItemEntity,
        api: TransactionApi
    ): SyncItemOutcome {
        val txn = runCatching { gson.fromJson(entity.payload, Transaction::class.java) }
            .getOrElse {
                return SyncItemOutcome.Failure(
                    entity.id, entity.type, SyncFailureKind.Parse, it.message.orEmpty()
                )
            }
        val response = api.addTransaction(txn.toSyncRequest())
        return interpret(entity, response)
    }

    /**
     * Maps an HTTP [Response] to a [SyncItemOutcome] with classification and
     * Last-Write-Wins handling.
     */
    private fun interpret(entity: SyncItemEntity, response: Response<*>): SyncItemOutcome {
        if (response.isSuccessful) {
            return SyncItemOutcome.Success(entity.id, entity.type)
        }
        val code = response.code()
        return when (code) {
            401, 403 -> SyncItemOutcome.Failure(
                entity.id, entity.type, SyncFailureKind.AuthExpired, "HTTP $code"
            )
            429 -> SyncItemOutcome.Failure(
                entity.id, entity.type, SyncFailureKind.Quota, "HTTP $code"
            )
            408, in 500..599 -> SyncItemOutcome.Failure(
                entity.id, entity.type, SyncFailureKind.ServerError, "HTTP $code"
            )
            // P2 — do not silently drop local changes on conflict; park as FAILED for user retry.
            409, 412, 422 -> SyncItemOutcome.Failure(
                entity.id,
                entity.type,
                SyncFailureKind.Conflict,
                "HTTP $code — server has a newer copy",
            )
            in 400..499 -> SyncItemOutcome.Failure(
                entity.id, entity.type, SyncFailureKind.ClientError, "HTTP $code"
            )
            else -> SyncItemOutcome.Failure(
                entity.id, entity.type, SyncFailureKind.Unknown, "HTTP $code"
            )
        }
    }

    private fun classify(t: Throwable): SyncFailureKind = when (t) {
        is UnknownHostException, is SocketTimeoutException, is SSLException -> SyncFailureKind.Network
        is IOException -> SyncFailureKind.Network
        is com.google.gson.JsonSyntaxException -> SyncFailureKind.Parse
        else -> SyncFailureKind.Unknown
    }

    /* ---------------- Public health API (preserved) ---------------- */

    suspend fun getPendingCount(): Int {
        val dbCount = syncDao?.getPendingCountOnce() ?: 0
        val memoryCount = currentInMemoryQueueSize()
        return dbCount + memoryCount
    }

    suspend fun getHealthSnapshot(): SyncHealthSnapshot {
        val dao = syncDao
        val pendingInDb = dao?.getPendingCountOnce() ?: 0
        val failedInDb = dao?.getFailedCountOnce() ?: 0
        val queuedInMemory = currentInMemoryQueueSize()
        return SyncHealthSnapshot(
            pendingInDb = pendingInDb,
            failedInDb = failedInDb,
            queuedInMemory = queuedInMemory,
            lastAttemptAt = lastAttemptAt,
            lastFailureReason = lastFailureReason
        )
    }

    suspend fun requeueFailedItems(): Int {
        val dao = syncDao ?: return 0
        // Backdate updatedAt so backoff doesn't gate the freshly-requeued rows.
        return dao.requeueFailedItems(0L)
    }

    /* ---------------- Internals ---------------- */

    private suspend fun commitDelete(
        dao: SyncDao,
        entity: SyncItemEntity,
        reasonOnFail: String
    ) {
        runCatching { dao.deleteById(entity.id) }.onFailure {
            // Atomicity guard: if delete throws, mark SYNCED so the row is
            // never re-uploaded to the server (which would create a duplicate).
            // It will be cleared by the existing `clearSynced()` housekeeping.
            Log.w(TAG, "deleteById failed ($reasonOnFail) — marking SYNCED to avoid dupe", it)
            runCatching {
                dao.updateStatusById(
                    id = entity.id,
                    status = SyncStatus.SYNCED.name,
                    retryCount = entity.retryCount,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
    }

    private suspend fun coalescePendingDuplicates(item: SyncItem) {
        val dao = syncDao ?: return
        runCatching {
            dao.deletePendingWithExactPayload(item.type, item.payload)
            when (item.type) {
                "TRANSACTION" -> {
                    val txn = gson.fromJson(item.payload, Transaction::class.java)
                    val ref = txn.txnRef.trim()
                    if (ref.isNotEmpty()) dao.deletePendingTransactionsByTxnRef(ref)
                }
                "CUSTOMER" -> {
                    val customer = gson.fromJson(item.payload, Customer::class.java)
                    if (customer.id > 0L) dao.deletePendingCustomersById(customer.id)
                }
            }
        }.onFailure {
            Log.w(TAG, "Sync dedup coalesce skipped", it)
        }
    }

    private suspend fun persistOrQueue(item: SyncItem) {
        val dao = syncDao
        if (dao == null) {
            parkInMemoryQueue(item)
            Log.w(TAG, "Sync DAO unavailable; item parked in memory queue.")
            return
        }
        coalescePendingDuplicates(item)
        runCatching {
            dao.insert(
                SyncItemEntity(
                    type = item.type,
                    payload = item.payload,
                    status = item.status.name
                )
            )
        }.onFailure {
            parkInMemoryQueue(item)
            Log.w(TAG, "Failed to persist sync item. Kept in memory queue.", it)
        }
    }

    private suspend fun parkInMemoryQueue(item: SyncItem) {
        queueMutex.withLock { queue.add(item) }
        SyncHealthMonitor.onInMemoryQueueChanged(currentInMemoryQueueSize())
        persistFallbackQueueSnapshot()
    }

    private suspend fun persistFallbackQueueSnapshot() {
        val ctx = appContext ?: return
        val snapshot = queueMutex.withLock { queue.toList() }
        SyncFallbackQueueStore.save(ctx, snapshot)
    }

    private suspend fun flushInMemoryQueueToDb() {
        val dao = syncDao ?: return
        val snapshot = queueMutex.withLock {
            val copy = queue.toList()
            queue.clear()
            copy
        }
        snapshot.forEach { item ->
            runCatching {
                coalescePendingDuplicates(item)
                dao.insert(
                    SyncItemEntity(
                        type = item.type,
                        payload = item.payload,
                        status = item.status.name
                    )
                )
            }.onFailure {
                queueMutex.withLock { queue.add(item) }
                Log.w(TAG, "Unable to flush in-memory queue item.", it)
            }
        }
        SyncHealthMonitor.onInMemoryQueueChanged(currentInMemoryQueueSize())
        persistFallbackQueueSnapshot()
    }

    private suspend fun currentInMemoryQueueSize(): Int = queueMutex.withLock { queue.size }
}
