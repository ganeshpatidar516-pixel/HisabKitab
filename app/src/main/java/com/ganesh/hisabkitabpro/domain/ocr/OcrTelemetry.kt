package com.ganesh.hisabkitabpro.domain.ocr

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.core.firebase.OpsTelemetryHub

/**
 * Wave 0 / 6 — non-PII OCR funnel logging. Never log raw OCR text, customer names, or amounts as
 * structured identifiers; only lengths, modes, coarse outcomes, and [BillAmountConfidence] / [BillAmountSource]
 * names for logcat / Crashlytics breadcrumbs.
 *
 * Phase-9 P2: routes through [OpsTelemetryHub] (`hk_ops_funnel`, domain=ocr).
 * Legacy `hk_ocr_funnel` is no longer emitted — use BigQuery on `hk_ops_funnel` where domain=ocr.
 */
object OcrTelemetry {
    private const val TAG = "HK_OCR"

    fun event(phase: String, details: Map<String, String> = emptyMap(), context: Context? = null) {
        val tail = if (details.isEmpty()) "" else " " + details.entries.joinToString(" ") { "${it.key}=${it.value}" }
        Log.i(TAG, "phase=$phase$tail")
        OpsTelemetryHub.log(context, OpsTelemetryHub.Domain.OCR, phase, details)
    }

    /** Optional duration for OCR phases (ms). */
    fun eventTimed(
        phase: String,
        durationMs: Long,
        details: Map<String, String> = emptyMap(),
        context: Context? = null,
    ) {
        val bucket = when {
            durationMs < 500 -> "lt_500ms"
            durationMs < 2_000 -> "500ms_2s"
            durationMs < 5_000 -> "2s_5s"
            else -> "gte_5s"
        }
        event(
            phase,
            details + mapOf(
                "duration_ms" to durationMs.coerceAtMost(120_000L).toString(),
                "duration_bucket" to bucket,
            ),
            context,
        )
    }
}
