package com.ganesh.hisabkitabpro.domain.logging

import android.util.Log

object SystemLogger {
    private const val TAG = "HisabKitabPro"

    fun logAction(action: String, details: String? = null) {
        Log.i(TAG, "Action: $action, Details(redacted): ${details?.length ?: 0} chars")
        // Logic to store logs in local database for audit trail
    }

    fun logError(error: String, exception: Throwable? = null) {
        Log.e(TAG, "Error: $error", exception)
    }

    fun logDebug(message: String) {
        Log.d(TAG, "Debug(redacted): ${message.length} chars")
    }
}
