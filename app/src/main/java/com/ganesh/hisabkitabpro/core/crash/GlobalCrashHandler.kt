package com.ganesh.hisabkitabpro.core.crash

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-level uncaught-exception handler — logs a sanitized local snapshot, then
 * forwards to the system / Crashlytics chain. Does not restart the app in a custom loop.
 */
@Singleton
class GlobalCrashHandler @Inject constructor(
    private val context: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun initialize() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "Uncaught exception on ${thread.name}; forwarding to default handler")

        recordCrashLoopMetric()
        saveCrashReport(throwable)

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun recordCrashLoopMetric() {
        val prefs = context.getSharedPreferences(CRASH_PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastCrashTime = prefs.getLong(KEY_LAST_CRASH_TIME, 0L)
        val count = prefs.getInt(KEY_CRASH_COUNT, 0)
        val finalCount = if (now - lastCrashTime > CRASH_WINDOW_MS) 1 else count + 1
        prefs.edit()
            .putInt(KEY_CRASH_COUNT, finalCount)
            .putLong(KEY_LAST_CRASH_TIME, now)
            .commit()
    }

    private fun saveCrashReport(throwable: Throwable) {
        val prefs = context.getSharedPreferences(CRASH_REPORT_PREFS, Context.MODE_PRIVATE)
        val raw = """
            Timestamp: ${System.currentTimeMillis()}
            Thread: ${Thread.currentThread().name}
            Error: ${throwable.localizedMessage}
            Stack: ${throwable.stackTraceToString()}
        """.trimIndent()
        val report = sanitizeForLocalCrashStore(raw)
        prefs.edit().putString(KEY_LAST_CRASH_FULL, report).commit()
    }

    private fun sanitizeForLocalCrashStore(text: String): String {
        var s = text
        s = s.replace(Regex("(?i)Bearer\\s+[\\w\\-._~+/]+=*"), "Bearer [REDACTED]")
        s = s.replace(
            Regex("(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|client_secret)\\s*[=:]\\s*\\S+"),
            "$1=[REDACTED]",
        )
        s = s.replace(Regex("AIza[0-9A-Za-z\\-_]{20,}"), "AIza[REDACTED]")
        s = s.replace(
            Regex("eyJ[A-Za-z0-9_\\-]+\\.eyJ[A-Za-z0-9_\\-]+\\.?[A-Za-z0-9_\\-]*"),
            "[JWT_REDACTED]",
        )
        s = s.replace(Regex("\\+91\\d{10}"), "+91**********")
        val max = 24 * 1024
        if (s.length > max) {
            s = s.substring(0, max) + "\n…[truncated]"
        }
        return s
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"
        private const val CRASH_REPORT_PREFS = "crash_reports"
        private const val KEY_LAST_CRASH_FULL = "last_crash_full"
        private const val CRASH_PREFS = "crash_prefs"
        private const val KEY_CRASH_COUNT = "crash_count"
        private const val KEY_LAST_CRASH_TIME = "last_crash_time"
        private const val CRASH_WINDOW_MS = 300_000L
    }
}
