package com.ganesh.hisabkitabpro.privacy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.WorkManager
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clears on-device user-owned content for account deletion / factory-reset style flows.
 *
 * Does **not** delete the SQLCipher key material in the Android Keystore (that would break
 * future DB opens on the same install); it empties all Room tables instead.
 */
@Singleton
class UserDataErasureEngine @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    suspend fun eraseAllOnDeviceUserOwnedData(
        db: AppDatabase,
        primaryPrefs: SharedPreferences,
    ) = withContext(Dispatchers.IO) {
        Log.i(TAG, "device_erasure_start")
        runCatching {
            WorkManager.getInstance(appContext).cancelUniqueWork(SyncWorker.WORK_NAME)
        }.onFailure { Log.w(TAG, "workmanager_cancel_failed", it) }

        wipeAuxiliaryPreferenceStores(appContext)
        primaryPrefs.edit().clear().commit()

        db.clearAllTables()

        wipeKnownCacheArtifacts(appContext)
        Log.i(TAG, "device_erasure_complete")
    }

    private fun wipeAuxiliaryPreferenceStores(context: Context) {
        for (name in AUXILIARY_PREF_FILES) {
            runCatching {
                context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            }.onFailure { Log.w(TAG, "aux_prefs_clear_failed name=$name", it) }
        }
    }

    private fun wipeKnownCacheArtifacts(context: Context) {
        val cache = context.cacheDir
        File(cache, "business_cards").deleteRecursivelyQuietly()
        File(cache, "db_backup").deleteRecursivelyQuietly()
        cache.listFiles()?.forEach { f ->
            if (f.isFile && f.name.startsWith(WHATSAPP_SHOWCASE_PREFIX)) {
                runCatching { f.delete() }
            }
        }
        context.externalCacheDir?.let { ext ->
            File(ext, "business_cards").deleteRecursivelyQuietly()
        }
    }

    private companion object {
        private const val TAG = "UserDataErasure"
        private const val WHATSAPP_SHOWCASE_PREFIX = "whatsapp_payment_showcase_"

        /**
         * Isolated preference files that can hold business / party / automation state.
         * (Primary `hisabkitab_prefs` is cleared separately.)
         */
        private val AUXILIARY_PREF_FILES = arrayOf(
            "ahre_reminder_prefs",
            "hisabkitab_guide_state",
            "customer_list_filter_prefs",
            "supplier_reconciliation_prefs",
            "supplier_profile_prefs",
            "supplier_credit_terms_prefs",
            "bill_item_quick_memory",
            "sync_worker_retry_guard",
            "crash_reports",
            "crash_prefs",
            "feature_recovery",
        )
    }
}

private fun File.deleteRecursivelyQuietly() {
    runCatching { deleteRecursively() }
}
