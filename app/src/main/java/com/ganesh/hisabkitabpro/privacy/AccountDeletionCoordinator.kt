package com.ganesh.hisabkitabpro.privacy

import android.content.SharedPreferences
import android.util.Log
import com.ganesh.hisabkitabpro.auth.AuthOpResult
import com.ganesh.hisabkitabpro.auth.AuthRepository
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.domain.cloud.CloudBusinessIdentity
import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AccountDeletionOutcome {
    data object CompletedFullRemoval : AccountDeletionOutcome()
    data object CompletedDeviceOnly : AccountDeletionOutcome()
    data class Failed(val userMessage: String) : AccountDeletionOutcome()
}

/**
 * Play Store–oriented pipeline: optional Firestore mirror purge (when signed in),
 * then full on-device erasure, then Firebase user deletion (when signed in).
 */
@Singleton
class AccountDeletionCoordinator @Inject constructor(
    private val database: AppDatabase,
    private val prefs: SharedPreferences,
    private val firebaseAuth: FirebaseAuth,
    private val cloudMirror: SelectiveCloudMirror,
    private val cloudBusinessIdentity: CloudBusinessIdentity,
    private val authRepository: AuthRepository,
    private val userDataErasureEngine: UserDataErasureEngine,
) {

    suspend fun runPlayStoreAccountDeletionPipeline(): AccountDeletionOutcome =
        withContext(Dispatchers.IO) {
            val user = firebaseAuth.currentUser
            val businessIdResult = runCatching { cloudBusinessIdentity.currentBusinessId() }
            val businessId = businessIdResult.getOrElse {
                Log.w(TAG, "business_id_resolve_failed")
                return@withContext AccountDeletionOutcome.Failed(
                    "Could not start deletion. Close other screens and try again."
                )
            }

            Log.i(TAG, "pipeline_start signed_in=${user != null}")

            if (user != null) {
                val purge = cloudMirror.purgeMirroredBusinessData(businessId)
                if (purge.isFailure) {
                    Log.w(TAG, "cloud_purge_failed", purge.exceptionOrNull())
                    return@withContext AccountDeletionOutcome.Failed(
                        "Could not remove cloud copies (check network). Nothing was erased on this device."
                    )
                }
            }

            try {
                userDataErasureEngine.eraseAllOnDeviceUserOwnedData(database, prefs)
            } catch (t: Throwable) {
                Log.w(TAG, "local_erase_failed", t)
                return@withContext AccountDeletionOutcome.Failed(
                    "Could not finish clearing this device. Try again when you have a stable connection."
                )
            }

            if (user != null) {
                return@withContext when (val del = authRepository.deleteFirebaseUser()) {
                    is AuthOpResult.Failure ->
                        AccountDeletionOutcome.Failed(
                            "This device was reset, but removing the cloud sign-in failed: ${del.userMessage}"
                        )
                    is AuthOpResult.Success ->
                        AccountDeletionOutcome.CompletedFullRemoval
                }
            }
            AccountDeletionOutcome.CompletedDeviceOnly
        }

    private companion object {
        private const val TAG = "AccountDeletion"
    }
}
