package com.ganesh.hisabkitabpro.domain.ocr

import android.util.Log

/**
 * Wave 0 / 6 — non-PII OCR funnel logging. Never log raw OCR text, customer names, or amounts as
 * structured identifiers; only lengths, modes, coarse outcomes, and [BillAmountConfidence] / [BillAmountSource]
 * names for logcat / Crashlytics breadcrumbs.
 */
object OcrTelemetry {
    private const val TAG = "HK_OCR"

    fun event(phase: String, details: Map<String, String> = emptyMap()) {
        val tail = if (details.isEmpty()) "" else " " + details.entries.joinToString(" ") { "${it.key}=${it.value}" }
        Log.i(TAG, "phase=$phase$tail")
    }
}
