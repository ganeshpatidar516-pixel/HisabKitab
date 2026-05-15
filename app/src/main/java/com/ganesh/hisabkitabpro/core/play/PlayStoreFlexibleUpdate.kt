package com.ganesh.hisabkitabpro.core.play

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import java.util.concurrent.TimeUnit

/**
 * Offers a **flexible** in-app update when the app is installed from Google Play.
 * Off-Play installs (debug, sideload) are skipped entirely so Play Core cannot crash the app.
 */
object PlayStoreFlexibleUpdate {
    private const val TAG = "PlayFlexUpdate"
    private const val PREFS = "play_flex_update_prefs"
    private const val KEY_LAST_PROMPT_MS = "last_prompt_ms"
    private val minIntervalMs = TimeUnit.DAYS.toMillis(2)

    const val REQUEST_CODE_FLEXIBLE_UPDATE: Int = 91001

    private fun installedFromPlayStore(activity: FragmentActivity): Boolean =
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.packageManager
                    .getInstallSourceInfo(activity.packageName)
                    .installingPackageName == "com.android.vending"
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getInstallerPackageName(activity.packageName) == "com.android.vending"
            }
        }.getOrElse { false }

    fun maybeStartFlexibleUpdate(activity: FragmentActivity) {
        if (!installedFromPlayStore(activity)) return

        runCatching {
            val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            if (now - prefs.getLong(KEY_LAST_PROMPT_MS, 0L) < minIntervalMs) return@runCatching

            val manager = AppUpdateManagerFactory.create(activity)
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    runCatching {
                        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@runCatching
                        if (!info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) return@runCatching
                        prefs.edit().putLong(KEY_LAST_PROMPT_MS, now).apply()
                        @Suppress("DEPRECATION")
                        manager.startUpdateFlowForResult(
                            info,
                            AppUpdateType.FLEXIBLE,
                            activity,
                            REQUEST_CODE_FLEXIBLE_UPDATE
                        )
                    }.onFailure { e ->
                        Log.w(TAG, "Flexible update flow not started", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "App update check skipped (Play Core unavailable)", e)
                }
        }.onFailure { e ->
            Log.w(TAG, "maybeStartFlexibleUpdate failed safely", e)
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE_FLEXIBLE_UPDATE) return
        if (resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "Flexible update dismissed or failed, resultCode=$resultCode data=$data")
        }
    }
}
