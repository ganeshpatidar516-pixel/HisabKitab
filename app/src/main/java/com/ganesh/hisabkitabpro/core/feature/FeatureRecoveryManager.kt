package com.ganesh.hisabkitabpro.core.feature

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureRecoveryManager @Inject constructor(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("feature_recovery", Context.MODE_PRIVATE)

    fun reportFeatureCrash(featureId: String) {
        val count = prefs.getInt("${featureId}_crash_count", 0) + 1
        prefs.edit().putInt("${featureId}_crash_count", count).apply()
        
        Log.w("FeatureRecovery", "Feature $featureId crashed $count times")
        
        if (count >= 3) {
            disableFeature(featureId)
            // Notify backend here
        }
    }

    fun isFeatureEnabled(featureId: String): Boolean {
        return prefs.getBoolean("${featureId}_enabled", true)
    }

    private fun disableFeature(featureId: String) {
        prefs.edit().putBoolean("${featureId}_enabled", false).apply()
        Log.e("FeatureRecovery", "Feature $featureId has been disabled due to repeated crashes")
    }

    fun resetFeature(featureId: String) {
        prefs.edit().apply {
            putInt("${featureId}_crash_count", 0)
            putBoolean("${featureId}_enabled", true)
        }.apply()
    }
}
