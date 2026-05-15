package com.ganesh.hisabkitabpro.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Phase-8 — non-fatal production signals for support dashboards.
 * No PII: only ids, coarse entity types, and short reason codes.
 */
object ProductionOpsTelemetry {

    private const val TAG = "ProductionOpsTelemetry"

    fun recordBillPdfNotReady(
        context: Context,
        transactionId: Long,
        billId: Long = -1L,
        source: String = "repository",
    ) {
        Log.w(TAG, "bill_pdf_not_ready txn=$transactionId bill=$billId source=$source")
        recordNonFatal(
            context,
            signal = "bill_pdf_not_ready",
            keys = mapOf(
                "bill_pdf_not_ready" to true,
                "bill_pdf_txn_id" to transactionId,
                "bill_pdf_bill_id" to billId,
                "bill_pdf_source" to source.take(40),
            ),
        )
    }

    fun recordSyncCycleDegraded(
        context: Context,
        permanentFailures: Int,
        authExpired: Boolean,
        attempted: Int,
    ) {
        if (permanentFailures <= 0 && !authExpired) return
        Log.w(
            TAG,
            "sync_cycle_degraded permanent=$permanentFailures authExpired=$authExpired attempted=$attempted",
        )
        recordNonFatal(
            context,
            signal = "sync_cycle_degraded",
            keys = mapOf(
                "sync_cycle_degraded" to true,
                "sync_cycle_permanent_failures" to permanentFailures,
                "sync_cycle_auth_expired" to authExpired,
                "sync_cycle_attempted" to attempted,
            ),
        )
    }

    fun recordBalanceCacheRepaired(context: Context, repairedCount: Int) {
        Log.i(TAG, "balance_cache_repaired count=$repairedCount")
        recordNonFatal(
            context,
            signal = "balance_cache_repaired",
            keys = mapOf(
                "balance_cache_repaired" to true,
                "balance_cache_repaired_count" to repairedCount,
            ),
        )
    }

    fun recordSyncCloudMirrorFailure(
        context: Context,
        entityType: String,
        reason: String,
    ) {
        val safeReason = reason.take(120)
        Log.w(TAG, "sync_cloud_mirror_failed type=$entityType reason=$safeReason")
        recordNonFatal(
            context,
            signal = "sync_cloud_mirror_failed",
            keys = mapOf(
                "sync_mirror_failed" to true,
                "sync_mirror_entity_type" to entityType.take(20),
                "sync_mirror_reason" to safeReason,
            ),
        )
    }

    private fun recordNonFatal(
        context: Context,
        signal: String,
        keys: Map<String, Any>,
    ) {
        runCatching {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!crashlytics.isCrashlyticsCollectionEnabled) return
            keys.forEach { (key, value) ->
                when (value) {
                    is Boolean -> crashlytics.setCustomKey(key, value)
                    is Int -> crashlytics.setCustomKey(key, value)
                    is Long -> crashlytics.setCustomKey(key, value)
                    is Float -> crashlytics.setCustomKey(key, value)
                    is Double -> crashlytics.setCustomKey(key, value)
                    else -> crashlytics.setCustomKey(key, value.toString().take(100))
                }
            }
            crashlytics.log(signal)
            crashlytics.recordException(ProductionOpsSignal(signal))
        }.onFailure {
            Log.w(TAG, "Crashlytics non-fatal skipped for $signal", it)
        }
    }

    private class ProductionOpsSignal(message: String) : Exception(message)
}
