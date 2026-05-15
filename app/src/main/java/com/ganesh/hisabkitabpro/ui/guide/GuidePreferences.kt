package com.ganesh.hisabkitabpro.ui.guide

import android.content.Context
import android.content.SharedPreferences

/**
 * First-run dashboard guide flags only (non-sensitive). Plain [SharedPreferences] avoids
 * Android Keystore / EncryptedSharedPreferences OEM issues that can crash or kill the process
 * shortly after launch. Isolated file name from ledger DB and from the old encrypted file.
 *
 * SCAFFOLDING (not a runtime caller yet) — paired with
 * [com.ganesh.hisabkitabpro.ui.guide.DashboardFirstRunGuideOverlay]. Preserved
 * intentionally per the "Preserve Working Systems" directive — see the overlay's
 * docstring for the intended wiring. Storage key `dashboard_guide_v1_done` is
 * versioned so future guide revisions can bump to `_v2_done` without forcing
 * already-onboarded users to see the overlay again.
 */
class GuidePreferences(context: Context) {

    private val appContext = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowDashboardGuide(): Boolean =
        runCatching { !prefs.getBoolean(KEY_DASHBOARD_GUIDE_DONE, false) }.getOrElse { false }

    fun markDashboardGuideDone() {
        runCatching { prefs.edit().putBoolean(KEY_DASHBOARD_GUIDE_DONE, true).apply() }
    }

    companion object {
        private const val PREFS_NAME = "hisabkitab_guide_state"
        private const val KEY_DASHBOARD_GUIDE_DONE = "dashboard_guide_v1_done"
    }
}
