package com.ganesh.hisabkitabpro.core.performance

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceGuard @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val lastCleanupAtMillis = AtomicLong(0L)

    /** Never run heavy I/O on the main thread — onTrimMemory/onLowMemory are main-thread callbacks. */
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun monitorMemory() {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMegs = memoryInfo.availMem / 1048576L
        val percentAvail = memoryInfo.availMem.toDouble() / memoryInfo.totalMem.toDouble() * 100.0

        if (memoryInfo.lowMemory || percentAvail < 10.0) {
            Log.w("PerformanceGuard", "Low memory detected! Available: $availableMegs MB ($percentAvail%)")
            cleanupScope.launch { performEmergencyCleanup() }
        }
    }

    private fun performEmergencyCleanup() {
        try {
            val now = System.currentTimeMillis()
            val last = lastCleanupAtMillis.get()
            if (now - last < 30_000L) return
            if (!lastCleanupAtMillis.compareAndSet(last, now) && now - lastCleanupAtMillis.get() < 30_000L) {
                return
            }
            context.cacheDir.listFiles()?.forEach { child ->
                runCatching {
                    // Preserve backup/restore safety files and directories.
                    if (child.name == "db_backup" || child.name == "db_restore") return@runCatching
                    if (child.isDirectory) {
                        child.listFiles()?.forEach { nested ->
                            // Keep only stale files (older than 24h) to reduce churn.
                            if (now - nested.lastModified() > 24L * 60L * 60L * 1000L) {
                                if (nested.isDirectory) nested.deleteRecursively() else nested.delete()
                            }
                        }
                    } else if (now - child.lastModified() > 24L * 60L * 60L * 1000L) {
                        child.delete()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("PerformanceGuard", "Cache cleanup failed", e)
        }
        Log.i("PerformanceGuard", "Emergency cleanup performed (background)")
    }
}
