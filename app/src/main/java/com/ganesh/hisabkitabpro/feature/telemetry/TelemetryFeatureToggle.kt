package com.ganesh.hisabkitabpro.feature.telemetry

import android.content.SharedPreferences

/**
 * User-facing opt-out for Firebase Crashlytics + Firebase Analytics.
 *
 * Default ON for both flags so existing installs see no behavioral change at
 * upgrade. The user can disable either independently from Settings →
 * "Diagnostics & Analytics", which:
 *   1. Persists the choice to SharedPreferences (`hisabkitab_prefs`).
 *   2. Calls Firebase's runtime collection-enabled APIs immediately so the
 *      change takes effect without an app restart.
 *
 * Mirrors the existing [com.ganesh.hisabkitabpro.feature.banksettle.BankAutoSettleFeatureToggle]
 * pattern. Reuses the same SharedPreferences file — no new I/O surface.
 */
class TelemetryFeatureToggle(
    private val prefs: SharedPreferences
) {
    fun isCrashReportingEnabled(): Boolean =
        prefs.getBoolean(KEY_CRASHLYTICS_V1, DEFAULT_ENABLED)

    fun isAnalyticsEnabled(): Boolean =
        prefs.getBoolean(KEY_ANALYTICS_V1, DEFAULT_ENABLED)

    fun setCrashReportingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CRASHLYTICS_V1, enabled).apply()
    }

    fun setAnalyticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ANALYTICS_V1, enabled).apply()
    }

    companion object {
        const val KEY_CRASHLYTICS_V1 = "feature_crashlytics_v1"
        const val KEY_ANALYTICS_V1 = "feature_analytics_v1"
        private const val DEFAULT_ENABLED = true
    }
}
