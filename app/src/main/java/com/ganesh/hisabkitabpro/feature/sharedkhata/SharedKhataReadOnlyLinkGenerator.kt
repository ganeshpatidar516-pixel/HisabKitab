package com.ganesh.hisabkitabpro.feature.sharedkhata

import com.ganesh.hisabkitabpro.network.NetworkConfig
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.ganesh.hisabkitabpro.network.api.SharedKhataApi
import com.ganesh.hisabkitabpro.network.api.SharedKhataPublishRequestDto
import com.ganesh.hisabkitabpro.network.api.SharedKhataRevokeRequestDto
import com.ganesh.hisabkitabpro.network.api.SharedKhataStatusDataDto
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Wrapper around shared-khata APIs — keeps `CustomerLedgerScreen` thin and
 * avoids duplicating Retrofit wiring across the project.
 *
 * All methods are **additive and non-invasive**. They never read or write the
 * merchant's local Room/SQLCipher schema; the only state they touch lives in
 * the dedicated shared-khata backend module.
 */
object SharedKhataReadOnlyLinkGenerator {

    data class PublishedLink(
        val shareToken: String,
        val otp: String,
        val viewUrl: String,
        val expiresAt: Long
    )

    private fun api(): SharedKhataApi = RetrofitClient.retrofit.create(SharedKhataApi::class.java)

    private fun resolveUrl(viewUrl: String?, viewPath: String?, shareToken: String): String {
        val base = NetworkConfig.BASE_URL.trimEnd('/')
        if (!viewUrl.isNullOrBlank()) return viewUrl.trim()
        val rawPath = (viewPath ?: "").trimStart('/').ifBlank {
            "api/v1/shared-khata/view-redirect/$shareToken"
        }
        return if (rawPath.startsWith("http")) rawPath else "$base/$rawPath"
    }

    /**
     * Publishes a read-only snapshot. Caller must ensure [SharedKhataFeatureToggle] is ON.
     * Safe to call from any dispatcher — this method shifts to IO internally.
     */
    suspend fun publishSnapshot(body: SharedKhataPublishRequestDto): Result<PublishedLink> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api().publish(body)
            val envelope = response.body()
            val data = envelope?.data
            if (!response.isSuccessful || envelope?.success != true || data == null) {
                val reason = envelope?.message?.takeIf { it.isNotBlank() } ?: "code=${response.code()}"
                throw IOException("Shared khata publish failed: $reason")
            }
            val url = resolveUrl(data.viewUrl, data.viewPath, data.shareToken)
            if (url.isBlank()) throw IOException("Shared khata publish failed: empty share link")
            val otp = data.otp.trim()
            if (otp.isBlank()) throw IOException("Shared khata publish failed: empty OTP")
            PublishedLink(
                shareToken = data.shareToken,
                otp = otp,
                viewUrl = url,
                expiresAt = data.expiresAt
            )
        }
    }

    /**
     * Revokes a previously published link — kills any live SSE viewers immediately.
     * No-op if the token does not exist or is already revoked.
     */
    suspend fun revokeShare(shareToken: String, merchantId: String? = null): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api().revoke(
                    SharedKhataRevokeRequestDto(shareToken = shareToken, merchantId = merchantId)
                )
                val envelope = response.body()
                val data = envelope?.data
                if (!response.isSuccessful || envelope?.success != true || data == null) {
                    val reason = envelope?.message?.takeIf { it.isNotBlank() } ?: "code=${response.code()}"
                    throw IOException("Shared khata revoke failed: $reason")
                }
                data.revokedAt
            }
        }

    /**
     * Reads the current lifecycle status of a share token.
     * Useful for UIs that want to show "active / expired / revoked / N views remaining".
     */
    suspend fun getStatus(shareToken: String): Result<SharedKhataStatusDataDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api().getStatus(shareToken)
                val envelope = response.body()
                val data = envelope?.data
                if (!response.isSuccessful || envelope?.success != true || data == null) {
                    val reason = envelope?.message?.takeIf { it.isNotBlank() } ?: "code=${response.code()}"
                    throw IOException("Shared khata status failed: $reason")
                }
                data
            }
        }
}
