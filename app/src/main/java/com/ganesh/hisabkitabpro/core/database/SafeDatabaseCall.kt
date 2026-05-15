package com.ganesh.hisabkitabpro.core.database

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🛡️ CHATGPT-LEVEL ERROR HANDLING
 * Global Try-Catch Wrapper for Database operations.
 */
suspend fun <T> safeDatabaseCall(
    tag: String = "HisabKitab_DB",
    dbCall: suspend () -> T
): T? {
    return withContext(Dispatchers.IO) {
        try {
            dbCall.invoke()
        } catch (e: Exception) {
            /**
             * 📝 CRASH REPORTING
             * Catching the full Stack Trace for diagnosis without crashing.
             */
            Log.e(tag, "Database operation failed", e)
            null
        }
    }
}

/**
 * 🛠️ COROUTINE EXCEPTION HANDLER
 * Ensures that background failures do not crash the UI.
 */
val globalExceptionHandler = CoroutineExceptionHandler { _, exception ->
    Log.e("HisabKitab_Global", "Background exception caught", exception)
}
