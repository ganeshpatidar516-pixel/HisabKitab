package com.ganesh.hisabkitabpro.core.firebase

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ganesh.hisabkitabpro.feature.telemetry.TelemetryFeatureToggle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Phase-9 P2 — unified ops funnel schema (Analytics) + local snapshot.
 * Crashlytics non-fatals remain in [ProductionOpsTelemetry].
 *
 * Event: `hk_ops_funnel` with `domain`, `phase`, and sanitized string attrs (no PII).
 */
object OpsTelemetryHub {

    private const val TAG = "HK_OPS"
    const val ANALYTICS_EVENT = "hk_ops_funnel"

    object Domain {
        const val SESSION = "session"
        const val OCR = "ocr"
        const val SYNC = "sync"
        const val INVOICE = "invoice"
        const val API = "api"
    }

    fun log(
        context: Context?,
        domain: String,
        phase: String,
        attrs: Map<String, String> = emptyMap(),
    ) {
        val safeDomain = domain.take(16)
        val safePhase = phase.take(40)
        val safeAttrs = sanitize(attrs)
        val tail = if (safeAttrs.isEmpty()) "" else " " + safeAttrs.entries.joinToString(" ") { "${it.key}=${it.value}" }
        Log.i(TAG, "domain=$safeDomain phase=$safePhase$tail")
        context?.applicationContext?.let { appCtx ->
            LocalOpsSnapshot.recordFunnel(appCtx, safeDomain, safePhase)
            logAnalyticsIfEnabled(appCtx, safeDomain, safePhase, safeAttrs)
        }
    }

    fun logSyncPhase(
        context: Context?,
        phase: String,
        attrs: Map<String, String> = emptyMap(),
    ) {
        context?.applicationContext?.let { LocalOpsSnapshot.recordSyncPhase(it, phase) }
        log(context, Domain.SYNC, phase, attrs)
    }

    private fun sanitize(attrs: Map<String, String>): Map<String, String> {
        val blocked = setOf("name", "phone", "note", "text", "amount", "customer")
        return attrs
            .filterKeys { key -> blocked.none { key.contains(it, ignoreCase = true) } }
            .mapValues { (_, v) -> v.take(100) }
            .entries
            .take(12)
            .associate { it.key.take(40) to it.value }
    }

    private fun logAnalyticsIfEnabled(
        context: Context,
        domain: String,
        phase: String,
        attrs: Map<String, String>,
    ) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val toggle = TelemetryFeatureToggle(
            context.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE),
        )
        if (!toggle.isAnalyticsEnabled()) return
        runCatching {
            val bundle = Bundle().apply {
                putString("domain", domain)
                putString("phase", phase)
                attrs.forEach { (k, v) -> putString(k, v) }
            }
            FirebaseAnalytics.getInstance(context).logEvent(ANALYTICS_EVENT, bundle)
        }.onFailure {
            Log.w(TAG, "Analytics funnel skipped for $domain/$phase", it)
        }
    }
}
