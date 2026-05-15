package com.ganesh.hisabkitabpro.core.firebase

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.feature.telemetry.TelemetryFeatureToggle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Initializes Firebase **only** from [android.app.Application] so core business modules stay untouched.
 *
 * **Important:** The repo ships a **placeholder** [app/google-services.json]. Firebase Performance (when present)
 * and other SDKs can call [com.google.firebase.installations.FirebaseInstallations.getId] on a worker thread;
 * an invalid API key throws [IllegalArgumentException] and **kills the whole app** — often seconds after launch
 * when the user touches the UI. We skip Firebase entirely until a real `google_api_key` is present.
 */
object FirebaseTelemetryBootstrap {

    private const val TAG = "FirebaseTelemetry"
    /** True when Firebase is live or permanently skipped (placeholder config). */
    private var initSettled = false
    private var crashChainInstalled = false

    private fun shouldSkipFirebaseForInvalidOrPlaceholderConfig(context: Context): Boolean {
        val app = context.applicationContext
        val pkg = app.packageName
        val res = app.resources
        val id = res.getIdentifier("google_api_key", "string", pkg)
        if (id == 0) {
            Log.w(TAG, "No google_api_key string resource — skipping Firebase (add google-services.json).")
            return true
        }
        val key = runCatching { res.getString(id).trim() }.getOrElse {
            Log.w(TAG, "Could not read google_api_key — skipping Firebase.", it)
            return true
        }
        if (key.isEmpty()) return true
        // Shipped placeholder and other dummy keys that Firebase Installations rejects at runtime.
        if (key.contains("000000000000000000000000")) return true
        if (key.equals("AIzaSy00000000000000000000000000000000000", ignoreCase = false)) return true
        return false
    }

    fun initialize(context: Context) {
        if (initSettled) return
        if (shouldSkipFirebaseForInvalidOrPlaceholderConfig(context)) {
            initSettled = true
            Log.w(
                TAG,
                "Firebase disabled: replace app/google-services.json with your Firebase Console file " +
                    "(valid API key). Ledger and all offline features work without Firebase."
            )
            return
        }
        val ok = runCatching {
            FirebaseApp.initializeApp(context.applicationContext)
            val toggle = readToggle(context)
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(toggle.isCrashReportingEnabled())
            crashlytics.setCustomKey("app_flavor", "hisabkitabpro")
            FirebaseAnalytics.getInstance(context.applicationContext)
                .setAnalyticsCollectionEnabled(toggle.isAnalyticsEnabled())
            Log.i(
                TAG,
                "Firebase initialized — crash=${toggle.isCrashReportingEnabled()}, " +
                    "analytics=${toggle.isAnalyticsEnabled()}"
            )
        }.onFailure { e ->
            Log.w(TAG, "Firebase init failed — will retry on next Application init", e)
        }.isSuccess
        if (ok) {
            initSettled = true
        }
    }

    /**
     * Apply a runtime change to the telemetry toggles. Called from Settings UI
     * so the user's choice takes effect immediately without an app restart.
     * No-op if Firebase was never initialized (placeholder google-services.json).
     */
    fun applyRuntimeChange(
        context: Context,
        crashReportingEnabled: Boolean,
        analyticsEnabled: Boolean
    ) {
        if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
        runCatching {
            FirebaseCrashlytics.getInstance()
                .setCrashlyticsCollectionEnabled(crashReportingEnabled)
        }
        runCatching {
            FirebaseAnalytics.getInstance(context.applicationContext)
                .setAnalyticsCollectionEnabled(analyticsEnabled)
        }
        Log.i(
            TAG,
            "Telemetry runtime change — crash=$crashReportingEnabled, analytics=$analyticsEnabled"
        )
    }

    private fun readToggle(context: Context): TelemetryFeatureToggle {
        val prefs = context.applicationContext
            .getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
        return TelemetryFeatureToggle(prefs)
    }

    /**
     * Chains Crashlytics **before** the existing handler so crashes still reach [GlobalCrashHandler] / system.
     * No-op if Firebase was not initialized (e.g. placeholder config).
     */
    fun wrapUncaughtExceptionHandlerForCrashlytics(context: Context) {
        if (FirebaseApp.getApps(context.applicationContext).isEmpty()) return
        if (crashChainInstalled) return
        val inner = Thread.getDefaultUncaughtExceptionHandler() ?: return
        if (inner is CrashlyticsForwardingHandler) return
        crashChainInstalled = true
        Thread.setDefaultUncaughtExceptionHandler(
            CrashlyticsForwardingHandler(inner)
        )
    }

    private class CrashlyticsForwardingHandler(
        private val inner: Thread.UncaughtExceptionHandler
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching {
                // Defense-in-depth: setCrashlyticsCollectionEnabled(false) already
                // makes Firebase drop the event, but we double-gate here so an
                // opted-out user can never have a crash reported even on a race.
                if (FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled) {
                    FirebaseCrashlytics.getInstance().recordException(e)
                }
            }
            inner.uncaughtException(t, e)
        }
    }
}
