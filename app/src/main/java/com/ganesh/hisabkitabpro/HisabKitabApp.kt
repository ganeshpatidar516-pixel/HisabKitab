package com.ganesh.hisabkitabpro

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.core.crash.GlobalCrashHandler
import com.ganesh.hisabkitabpro.core.firebase.FirebaseTelemetryBootstrap
import com.ganesh.hisabkitabpro.core.firebase.OpsTelemetryHub
import com.ganesh.hisabkitabpro.core.performance.PerformanceGuard
import com.ganesh.hisabkitabpro.core.storage.FileProviderStorageMigration
import com.ganesh.hisabkitabpro.domain.profile.LogoNormalizationMigration
import com.ganesh.hisabkitabpro.network.FirebaseRetrofitAuthBridge
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.migration.SupplierPartyCityBackfill
import com.ganesh.hisabkitabpro.addon.reminder.ReminderEngine
import com.ganesh.hisabkitabpro.domain.ledger.BalanceCacheReconciler
import com.ganesh.hisabkitabpro.domain.ledger.BalanceCacheRepairToggle
import com.ganesh.hisabkitabpro.domain.sync.SyncEngine
import com.ganesh.hisabkitabpro.domain.sync.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HISABKITAB PRO - 🏰 THE GLOBAL ENGINE
 * Root Level Pre-Warming for 100% Stability.
 */
@HiltAndroidApp
class HisabKitabApp : Application() {

    @Inject
    lateinit var globalCrashHandler: GlobalCrashHandler

    @Inject
    lateinit var performanceGuard: PerformanceGuard

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var reminderEngine: ReminderEngine

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate() {
        val startupStart = SystemClock.elapsedRealtime()
        super.onCreate()
        RetrofitClient.install(this)
        FirebaseTelemetryBootstrap.initialize(this)
        AppLocaleManager.applyPersistedLocale(this)
        runCatching { globalCrashHandler.initialize() }
            .onFailure { Log.e("HisabKitabApp", "Crash handler init failed", it) }
        FirebaseTelemetryBootstrap.wrapUncaughtExceptionHandlerForCrashlytics(this)
        runCatching { reminderEngine.ensureScheduled() }
            .onFailure { Log.e("HisabKitabApp", "Reminder engine schedule failed", it) }
        runCatching { SyncEngine.initialize(database.syncDao(), this) }
            .onFailure { Log.e("HisabKitabApp", "Sync engine init failed", it) }
        // Sync Health Monitor is initialized inside SyncEngine.initialize(),
        // but we re-bind defensively here so the StateFlow is hot from t=0
        // even if the engine init fails.
        runCatching {
            com.ganesh.hisabkitabpro.domain.sync.SyncHealthMonitor.initialize(database.syncDao(), this)
        }.onFailure { Log.e("HisabKitabApp", "Sync health monitor init failed", it) }
        runCatching { SyncWorker.ensureScheduled(this) }
            .onFailure { Log.e("HisabKitabApp", "Sync worker schedule failed", it) }
        // Restore FastAPI Bearer for returning users (cold start) without touching ledger DB.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { FirebaseRetrofitAuthBridge.syncBearerFromFirebase(firebaseAuth) }
                .onFailure { Log.w("HisabKitabApp", "Retrofit bearer cold-start sync skipped", it) }
        }
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { LogoNormalizationMigration.runIfNeeded(this@HisabKitabApp) }
                .onFailure { Log.w("HisabKitabApp", "Logo normalization migration skipped", it) }
        }
        Log.i("HisabKitabApp", "Super command feature keeps user-defined toggle state.")
        Log.i(
            "HisabKitabApp",
            "ReminderEngine: periodic work enqueued (uniqueName=${ReminderEngine.WORK_NAME}, worker=ReminderEscalationWorker)"
        )

        // ✅ CRITICAL FIX: Pre-warm Database on App Launch
        // This initializes SQLCipher on a background thread so the first click
        // doesn't cause a freeze.
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { FileProviderStorageMigration.runIfNeeded(this@HisabKitabApp, database) }
                .onFailure { Log.w("HisabKitabApp", "Storage layout migration skipped", it) }
            runCatching { SyncEngine.restorePersistedFallbackQueue() }
                .onFailure { Log.w("HisabKitabApp", "Sync fallback queue restore skipped", it) }
            runCatching { SupplierPartyCityBackfill.runIfNeeded(this@HisabKitabApp, database) }
                .onFailure { Log.w("HisabKitabApp", "Supplier city column backfill skipped", it) }
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.openHelper.writableDatabase.query("SELECT 1").use { c ->
                    c.moveToFirst()
                }
                val balanceRepairToggle = BalanceCacheRepairToggle(
                    getSharedPreferences("hisabkitab_prefs", MODE_PRIVATE),
                )
                BalanceCacheReconciler.logDriftIfAny(
                    appContext = this@HisabKitabApp,
                    customerDao = database.customerDao(),
                    transactionDao = database.transactionDao(),
                )
                BalanceCacheReconciler.repairDriftIfEnabled(
                    appContext = this@HisabKitabApp,
                    database = database,
                    toggle = balanceRepairToggle,
                )
            } catch (e: Exception) {
                Log.w("HisabKitabApp", "DB pre-warm skipped", e)
            }
        }

        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                    performanceGuard.monitorMemory()
                }
            }

            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() {
                performanceGuard.monitorMemory()
            }
        })
        val startupMs = SystemClock.elapsedRealtime() - startupStart
        Log.i("HisabKitabApp", "Startup init completed in $startupMs ms")
        OpsTelemetryHub.log(
            this,
            OpsTelemetryHub.Domain.SESSION,
            "startup_ok",
            mapOf("startup_ms" to startupMs.toString()),
        )
    }
}
