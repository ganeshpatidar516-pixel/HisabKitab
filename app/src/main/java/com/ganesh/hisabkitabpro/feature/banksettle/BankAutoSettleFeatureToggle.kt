package com.ganesh.hisabkitabpro.feature.banksettle

import android.content.SharedPreferences

/**
 * Bank SMS → match suggestion layer. Default OFF — no notifications until user opts in (Settings beta).
 */
class BankAutoSettleFeatureToggle(
    private val prefs: SharedPreferences
) {
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_BANK_AUTO_SETTLE_V1, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BANK_AUTO_SETTLE_V1, enabled).apply()
    }

    companion object {
        const val KEY_BANK_AUTO_SETTLE_V1 = "feature_bank_auto_settle_v1"
    }
}
