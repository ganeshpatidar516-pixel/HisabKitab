package com.ganesh.hisabkitabpro.core.feature

/**
 * Tracks which experimental [SafeFeature] module is active — read on uncaught crash only.
 * Sacred ledger screens must not set this.
 */
object ActiveFeatureTracker {

    @Volatile
    var activeFeatureId: String? = null
        private set

    fun setActive(featureId: String?) {
        activeFeatureId = featureId
    }
}
