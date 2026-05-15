package com.ganesh.hisabkitabpro.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class SharedKhataLinePayload(
    @SerializedName("amount_paise") val amountPaise: Long,
    val type: String,
    val note: String? = null,
    @SerializedName("created_at") val createdAt: Long
)

/**
 * Publish payload. [merchantId] and [maxSuccessfulViews] are additive and optional —
 * older builds that don't send them still work because the backend treats them
 * as defaults ("anonymous" + 3 views). Wire-format is unchanged for v1 callers.
 */
data class SharedKhataPublishRequestDto(
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("customer_local_id") val customerLocalId: String,
    @SerializedName("balance_paise") val balancePaise: Long,
    val lines: List<SharedKhataLinePayload>,
    @SerializedName("ttl_hours") val ttlHours: Int = 24,
    @SerializedName("merchant_id") val merchantId: String? = null,
    @SerializedName("business_id") val businessId: String? = null,
    @SerializedName("created_by_user_id") val createdByUserId: String? = null,
    @SerializedName("max_successful_views") val maxSuccessfulViews: Int = 3
)

data class SharedKhataPublishDataDto(
    @SerializedName("share_token") val shareToken: String,
    val otp: String,
    @SerializedName("view_url") val viewUrl: String?,
    @SerializedName("view_path") val viewPath: String?,
    @SerializedName("expires_at") val expiresAt: Long
)

data class SharedKhataRevokeRequestDto(
    @SerializedName("share_token") val shareToken: String,
    @SerializedName("merchant_id") val merchantId: String? = null
)

data class SharedKhataRevokeDataDto(
    @SerializedName("share_token") val shareToken: String,
    @SerializedName("revoked_at") val revokedAt: Long
)

data class SharedKhataStatusDataDto(
    @SerializedName("share_token") val shareToken: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("balance_paise") val balancePaise: Long,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("revoked_at") val revokedAt: Long,
    @SerializedName("successful_view_count") val successfulViewCount: Int,
    @SerializedName("max_successful_views") val maxSuccessfulViews: Int,
    @SerializedName("line_count") val lineCount: Int
)

data class SharedKhataApiEnvelope<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: Any? = null
)

/**
 * Online Shared Khata wire-protocol — server-side endpoints implemented in
 * `HisabKitabBackend/shared_khata/`. All methods are additive; older callers
 * that only used [publish] continue to work unchanged.
 */
interface SharedKhataApi {
    @POST("api/v1/shared-khata/publish")
    suspend fun publish(
        @Body body: SharedKhataPublishRequestDto
    ): Response<SharedKhataApiEnvelope<SharedKhataPublishDataDto>>

    @POST("api/v1/shared-khata/revoke")
    suspend fun revoke(
        @Body body: SharedKhataRevokeRequestDto
    ): Response<SharedKhataApiEnvelope<SharedKhataRevokeDataDto>>

    @GET("api/v1/shared-khata/status/{shareToken}")
    suspend fun getStatus(
        @Path("shareToken") shareToken: String
    ): Response<SharedKhataApiEnvelope<SharedKhataStatusDataDto>>
}
