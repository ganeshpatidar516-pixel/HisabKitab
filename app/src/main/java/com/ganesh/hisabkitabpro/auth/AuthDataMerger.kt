package com.ganesh.hisabkitabpro.auth

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-destructive bridge between the user's local ledger data and their
 * Firebase Auth account.
 *
 * DESIGN NOTES (CRITICAL):
 * - This class **does not** read, write, or delete any rows from the Room/
 *   SQLCipher database. The existing customer / transaction / bill flows are
 *   100% untouched.
 * - The "merge" semantics required by the spec ("automatically merge offline
 *   data with the user's permanent account, no data loss") are implemented by
 *   *stamping* the signed-in UID onto the local profile in [SharedPreferences].
 *   That stamp is later picked up by the existing [com.ganesh.hisabkitabpro.domain.sync.SyncEngine]
 *   / [com.ganesh.hisabkitabpro.domain.backup.CloudBackupManager] so any
 *   subsequent cloud backup is scoped to the right account — without ever
 *   having to copy or move the ledger rows themselves.
 * - If a different UID was previously signed in on the same device, we keep
 *   the previous UID under [PREF_PREVIOUS_UID] so a future migration job can
 *   reconcile it. We never silently overwrite local data.
 */
@Singleton
class AuthDataMerger @Inject constructor(
    private val prefs: SharedPreferences
) {

    /**
     * Called immediately after a successful sign-in. Safe to call repeatedly.
     */
    fun onSignInSuccess(uid: String) {
        if (uid.isBlank()) return
        runCatching {
            val previous = prefs.getString(PREF_CURRENT_UID, null)
            prefs.edit {
                if (!previous.isNullOrBlank() && previous != uid) {
                    putString(PREF_PREVIOUS_UID, previous)
                }
                putString(PREF_CURRENT_UID, uid)
                if (!prefs.contains(PREF_FIRST_SIGN_IN_AT)) {
                    putLong(PREF_FIRST_SIGN_IN_AT, System.currentTimeMillis())
                }
                putLong(PREF_LAST_SIGN_IN_AT, System.currentTimeMillis())
            }
        }.onFailure { Log.w(TAG, "Failed to stamp uid into prefs", it) }
    }

    /** Returns the most recently stamped UID, if any. */
    fun currentStampedUid(): String? = prefs.getString(PREF_CURRENT_UID, null)
            ?.takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "AuthDataMerger"

        const val PREF_CURRENT_UID = "auth.current_uid"
        const val PREF_PREVIOUS_UID = "auth.previous_uid"
        const val PREF_FIRST_SIGN_IN_AT = "auth.first_sign_in_at"
        const val PREF_LAST_SIGN_IN_AT = "auth.last_sign_in_at"
    }
}
