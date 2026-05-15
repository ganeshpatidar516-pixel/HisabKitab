package com.ganesh.hisabkitabpro.domain.ocr

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ganesh.hisabkitabpro.feature.telemetry.TelemetryFeatureToggle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Wave 0 / 6 — non-PII OCR funnel logging. Never log raw OCR text, customer names, or amounts as
 * structured identifiers; only lengths, modes, coarse outcomes, and [BillAmountConfidence] / [BillAmountSource]
 * names for logcat / Crashlytics breadcrumbs.
 *
 * Firebase Analytics events are gated by [TelemetryFeatureToggle] and skipped when Firebase is not initialized.
 */
object OcrTelemetry {
    private const val TAG = "HK_OCR"
    private const val ANALYTICS_EVENT = "hk_ocr_funnel"

    fun event(phase: String, details: Map<String, String> = emptyMap(), context: Context? = null) {
        val tail = if (details.isEmpty()) "" else " " + details.entries.joinToString(" ") { "${it.key}=${it.value}" }
        Log.i(TAG, "phase=$phase$tail")
        context?.applicationContext?.let { logAnalyticsIfEnabled(it, phase, details) }
    }

    private fun logAnalyticsIfEnabled(context: Context, phase: String, details: Map<String, String>) {
        if (FirebaseApp.getApps(context).isEmpty()) return
        val toggle = TelemetryFeatureToggle(
            context.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE),
        )
        if (!toggle.isAnalyticsEnabled()) return
        runCatching {
            val bundle = Bundle().apply {
                putString("phase", phase.take(40))
                details.forEach { (k, v) ->
                    putString(k.take(40), v.take(100))
                }
            }
            FirebaseAnalytics.getInstance(context).logEvent(ANALYTICS_EVENT, bundle)
        }
    }
}
