package com.ganesh.hisabkitabpro.feature.sharedkhata

import android.content.SharedPreferences

/**
 * G-01: Online shared khata (read-only link).
 * Default **OFF** (release governance): users opt in via Settings → beta; avoids surprise network calls.
 */
class SharedKhataFeatureToggle(
    private val prefs: SharedPreferences
) {
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_SHARED_KHATA_V1, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHARED_KHATA_V1, enabled).apply()
    }

    companion object {
        const val KEY_SHARED_KHATA_V1 = "feature_shared_khata_v1"
    }
}
