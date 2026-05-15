package com.ganesh.hisabkitabpro.integrity

import android.content.Context
import android.util.Base64
import android.util.Log
import com.ganesh.hisabkitabpro.BuildConfig
import com.ganesh.hisabkitabpro.network.api.DeviceAttestationApi
import com.ganesh.hisabkitabpro.network.api.IntegrityVerifyRequest
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

data class IntegrityArtifact(
    val token: String,
    /** Same nonce passed to Play Integrity — backend must treat it as opaque and bind to the token. */
    val nonce: String,
)

/**
 * Phase 8 — Play Integrity token acquisition + optional backend hand-off.
 *
 * Set [BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER] to your numeric Play / GCP project number.
 */
@Singleton
class PlayIntegrityRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceAttestationApi: DeviceAttestationApi,
) {

    suspend fun requestIntegrityArtifact(): Result<IntegrityArtifact> = withContext(Dispatchers.IO) {
        val projectNumber = BuildConfig.PLAY_INTEGRITY_CLOUD_PROJECT_NUMBER.trim()
        if (projectNumber.isEmpty() || projectNumber == "0") {
            return@withContext Result.failure(
                IllegalStateException("Play Integrity cloud project number not configured (BuildConfig).")
            )
        }
        runCatching {
            val nonce = randomNonce()
            val manager = IntegrityManagerFactory.create(context)
            val req = IntegrityTokenRequest.builder()
                .setCloudProjectNumber(projectNumber.toLong())
                .setNonce(nonce)
                .build()
            val token = Tasks.await(manager.requestIntegrityToken(req)).token()
            if (token.isBlank()) error("empty_integrity_token")
            IntegrityArtifact(token = token, nonce = nonce)
        }.fold(
            onSuccess = { Result.success(it) },
            onFailure = { t ->
                Log.w(TAG, "integrity_token_failed", t)
                Result.failure(t)
            },
        )
    }

    suspend fun submitArtifactToBackend(artifact: IntegrityArtifact): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            deviceAttestationApi.submitIntegrityToken(
                IntegrityVerifyRequest(
                    integrityToken = artifact.token,
                    nonce = artifact.nonce,
                    packageName = context.packageName,
                )
            )
        }.fold(
            onSuccess = { resp ->
                if (resp.isSuccessful || resp.code() == 404) Result.success(Unit)
                else Result.failure(IllegalStateException("integrity_verify_http_${resp.code()}"))
            },
            onFailure = { Result.failure(it) },
        )
    }

    suspend fun requestAndSubmitToBackend(): Result<Unit> {
        val artifact = requestIntegrityArtifact().getOrElse { return Result.failure(it) }
        return submitArtifactToBackend(artifact)
    }

    private fun randomNonce(): String {
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(raw, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    companion object {
        private const val TAG = "HK_PlayIntegrity"
    }
}
