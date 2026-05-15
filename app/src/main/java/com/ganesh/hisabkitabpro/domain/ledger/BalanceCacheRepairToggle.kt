package com.ganesh.hisabkitabpro.domain.ledger

import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.BuildConfig

/**
 * Phase-8 P2 — opt-in balance cache repair. Default OFF; user enables in Cloud settings.
 * Compile-time [BuildConfig.BALANCE_CACHE_AUTO_REPAIR_ALLOWED] can disable the feature entirely.
 */
class BalanceCacheRepairToggle(
    private val prefs: SharedPreferences,
) {
    fun isAutoRepairEnabled(): Boolean =
        BuildConfig.BALANCE_CACHE_AUTO_REPAIR_ALLOWED &&
            prefs.getBoolean(KEY_AUTO_REPAIR_V1, DEFAULT_AUTO_REPAIR)

    fun setAutoRepairEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REPAIR_V1, enabled).apply()
    }

    fun isFeatureAvailable(): Boolean = BuildConfig.BALANCE_CACHE_AUTO_REPAIR_ALLOWED

    companion object {
        const val KEY_AUTO_REPAIR_V1 = "feature_balance_cache_auto_repair_v1"
        private const val DEFAULT_AUTO_REPAIR = false
    }
}
