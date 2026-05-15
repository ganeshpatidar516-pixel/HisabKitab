package com.ganesh.hisabkitabpro.domain.profile

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths

/**
 * One-time cold-start migration: legacy raw `business_logo.png` copies are re-encoded into the
 * canonical [MerchantLogoPipeline.LOGO_MASTER_BOX] letterboxed master. No Room migration.
 *
 * Version is bumped only after success, or when no file / already canonical — so failures retry.
 */
object LogoNormalizationMigration {

    private const val TAG = "LogoNormMigration"
    private const val PREFS_NAME = "hisabkitab_profile_migrations"
    private const val KEY_VERSION = "merchant_logo_normalize_v1"
    private const val DONE_VERSION = 1

    fun runIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getInt(KEY_VERSION, 0) >= DONE_VERSION) return

        runCatching {
            val logoFile = AppStoragePaths.businessLogoFile(context)
            if (!logoFile.isFile || logoFile.length() == 0L) {
                prefs.edit().putInt(KEY_VERSION, DONE_VERSION).apply()
                return@runCatching
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(logoFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                prefs.edit().putInt(KEY_VERSION, DONE_VERSION).apply()
                return@runCatching
            }

            if (bounds.outWidth == MerchantLogoPipeline.LOGO_MASTER_BOX &&
                bounds.outHeight == MerchantLogoPipeline.LOGO_MASTER_BOX
            ) {
                prefs.edit().putInt(KEY_VERSION, DONE_VERSION).apply()
                return@runCatching
            }

            val ok = MerchantLogoPipeline.normalizeExistingLogoFile(logoFile)
            if (ok) {
                prefs.edit().putInt(KEY_VERSION, DONE_VERSION).apply()
            } else {
                Log.w(TAG, "Legacy logo normalize failed; will retry on next launch.")
            }
        }.onFailure { e ->
            Log.e(TAG, "Logo normalization migration error", e)
        }
    }
}
