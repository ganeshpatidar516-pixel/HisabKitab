package com.ganesh.hisabkitabpro.core.firebase

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Phase-8/9 — non-fatal production signals for support dashboards.
 * No PII: only ids, coarse entity types, paths, and short reason codes.
 */
object ProductionOpsTelemetry {

    private const val TAG = "ProductionOpsTelemetry"
    private const val SLOW_API_MS = 10_000L

    /** Call once after Firebase init so every crash/non-fatal is tagged with build identity. */
    fun applySessionKeys(context: Context) {
        runCatching {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!crashlytics.isCrashlyticsCollectionEnabled) return
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE)
            crashlytics.setCustomKey("version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
        }
    }

    fun recordInvoiceSaveOutcome(
        context: Context,
        success: Boolean,
        pdfReady: Boolean,
        source: String,
        billId: Long = -1L,
        transactionId: Long = -1L,
    ) {
        if (success && pdfReady) {
            Log.i(TAG, "invoice_save_ok source=${source.take(40)}")
            OpsTelemetryHub.log(
                context,
                OpsTelemetryHub.Domain.INVOICE,
                "bill_save_ok",
                mapOf("source" to source.take(40)),
            )
            return
        }
        Log.w(
            TAG,
            "invoice_save_issue success=$success pdfReady=$pdfReady source=$source bill=$billId txn=$transactionId",
        )
        OpsTelemetryHub.log(
            context,
            OpsTelemetryHub.Domain.INVOICE,
            if (success) "bill_save_pdf_missing" else "bill_save_failed",
            mapOf(
                "pdf_ready" to pdfReady.toString(),
                "source" to source.take(40),
            ),
        )
        recordNonFatal(
            context,
            signal = "invoice_save_issue",
            keys = mapOf(
                "invoice_save_issue" to true,
                "invoice_save_success" to success,
                "invoice_save_pdf_ready" to pdfReady,
                "invoice_save_source" to source.take(40),
                "invoice_save_bill_id" to billId,
                "invoice_save_txn_id" to transactionId,
            ),
        )
    }

    fun recordApiCall(
        context: Context,
        method: String,
        path: String,
        httpCode: Int,
        durationMs: Long,
        errorKind: String? = null,
    ) {
        val safePath = path.take(80)
        val bucket = latencyBucket(durationMs)
        Log.d(TAG, "api $method $safePath code=$httpCode ${durationMs}ms bucket=$bucket")
        val degraded = httpCode >= 500 || httpCode < 0 || durationMs >= SLOW_API_MS
        if (!degraded) return
        OpsTelemetryHub.log(
            context,
            OpsTelemetryHub.Domain.API,
            "call_degraded",
            mapOf(
                "http_code" to httpCode.toString(),
                "latency_bucket" to bucket,
                "error_kind" to (errorKind?.take(40) ?: ""),
            ),
        )
        recordNonFatal(
            context,
            signal = "api_call_degraded",
            keys = mapOf(
                "api_call_degraded" to true,
                "api_method" to method.take(8),
                "api_path" to safePath,
                "api_http_code" to httpCode,
                "api_duration_ms" to durationMs.coerceAtMost(120_000L),
                "api_latency_bucket" to bucket,
                "api_error_kind" to (errorKind?.take(40) ?: ""),
            ),
        )
    }

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
        OpsTelemetryHub.log(
            context,
            OpsTelemetryHub.Domain.SYNC,
            "cycle_degraded",
            mapOf(
                "permanent_failures" to permanentFailures.toString(),
                "auth_expired" to authExpired.toString(),
                "attempted" to attempted.toString(),
            ),
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

    private fun latencyBucket(durationMs: Long): String = when {
        durationMs < 500 -> "lt_500ms"
        durationMs < 2_000 -> "500ms_2s"
        durationMs < 5_000 -> "2s_5s"
        durationMs < 10_000 -> "5s_10s"
        else -> "gte_10s"
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
