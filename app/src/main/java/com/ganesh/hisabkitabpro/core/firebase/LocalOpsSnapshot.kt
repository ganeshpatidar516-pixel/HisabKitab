package com.ganesh.hisabkitabpro.core.firebase

import android.content.Context
import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.BuildConfig

/**
 * Phase-9 P2 — local ops rollup for Settings (no PII). Mirrors last funnel + build identity.
 */
object LocalOpsSnapshot {

    private const val PREFS = "hk_ops_snapshot"
    private const val KEY_LAST_DOMAIN = "last_domain"
    private const val KEY_LAST_PHASE = "last_phase"
    private const val KEY_LAST_AT_MS = "last_at_ms"
    private const val KEY_LAST_SYNC_PHASE = "last_sync_phase"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recordFunnel(context: Context, domain: String, phase: String) {
        prefs(context).edit()
            .putString(KEY_LAST_DOMAIN, domain.take(16))
            .putString(KEY_LAST_PHASE, phase.take(40))
            .putLong(KEY_LAST_AT_MS, System.currentTimeMillis())
            .apply()
    }

    fun recordSyncPhase(context: Context, phase: String) {
        prefs(context).edit()
            .putString(KEY_LAST_SYNC_PHASE, phase.take(20))
            .apply()
    }

    data class View(
        val versionName: String,
        val versionCode: Int,
        val buildType: String,
        val lastDomain: String?,
        val lastPhase: String?,
        val lastAtMs: Long,
        val lastSyncPhase: String?,
    )

    fun read(context: Context): View {
        val p = prefs(context)
        return View(
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE,
            buildType = if (BuildConfig.DEBUG) "debug" else "release",
            lastDomain = p.getString(KEY_LAST_DOMAIN, null),
            lastPhase = p.getString(KEY_LAST_PHASE, null),
            lastAtMs = p.getLong(KEY_LAST_AT_MS, 0L),
            lastSyncPhase = p.getString(KEY_LAST_SYNC_PHASE, null),
        )
    }
}
