package com.ganesh.hisabkitabpro.commandos

import android.content.SharedPreferences

class SuperCommandFeatureToggle(
    private val prefs: SharedPreferences
) {
    /**
     * Default enabled for production assistant path.
     * Persisted user/admin choice still overrides this.
     */
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_SUPER_COMMAND_V1, true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SUPER_COMMAND_V1, enabled).apply()
    }

    companion object {
        private const val KEY_SUPER_COMMAND_V1 = "feature_super_command_v1"
    }
}
