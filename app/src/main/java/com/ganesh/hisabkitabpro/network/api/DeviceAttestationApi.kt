package com.ganesh.hisabkitabpro.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Backend hook for Play Integrity (Phase 8). Server must verify the token with Google;
 * this client only POSTs the opaque token + nonce metadata.
 */
data class IntegrityVerifyRequest(
    @SerializedName("integrity_token") val integrityToken: String,
    @SerializedName("nonce") val nonce: String,
    @SerializedName("package_name") val packageName: String,
)

interface DeviceAttestationApi {
    @POST("api/v1/device/integrity-verify")
    suspend fun submitIntegrityToken(@Body body: IntegrityVerifyRequest): Response<Unit>
}
