package com.ganesh.hisabkitabpro.network.api

import com.ganesh.hisabkitabpro.domain.model.AppSettings
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface SettingsApi {
    @GET("api/v1/settings")
    suspend fun getSettings(): Response<AppSettings>

    @POST("api/v1/settings/update")
    suspend fun updateSettings(@Body settings: AppSettings): Response<Unit>

    @GET("api/v1/business/profile")
    suspend fun getBusinessProfile(): Response<BusinessProfile>

    @POST("api/v1/business/update")
    suspend fun updateBusinessProfile(@Body profile: BusinessProfile): Response<Unit>

    @Multipart
    @POST("api/v1/business/logo")
    suspend fun uploadLogo(@Part logo: MultipartBody.Part): Response<Map<String, String>>

    @GET("api/v1/invoice/templates")
    suspend fun getInvoiceTemplates(): Response<List<Map<String, Any>>>

    @POST("api/v1/invoice/template/select")
    suspend fun selectInvoiceTemplate(@Body data: Map<String, String>): Response<Unit>

    @GET("api/v1/settings/taxes")
    suspend fun getTaxSettings(): Response<Map<String, Any>>

    @POST("api/v1/settings/taxes")
    suspend fun updateTaxSettings(@Body data: Map<String, Any>): Response<Unit>

    @POST("api/v1/settings/voice")
    suspend fun toggleVoiceAssistant(@Body data: Map<String, Boolean>): Response<Unit>

    @POST("api/v1/settings/ai-suggestions")
    suspend fun toggleAiSuggestions(@Body data: Map<String, Boolean>): Response<Unit>

    @POST("api/v1/settings/pin")
    suspend fun updateSecurityPin(@Body data: Map<String, String>): Response<Unit>

    @POST("api/v1/settings/theme")
    suspend fun updateTheme(@Body data: Map<String, String>): Response<Unit>

    @POST("api/v1/sync/backup")
    suspend fun performBackup(@Body data: Map<String, Any>): Response<Unit>

    @GET("api/v1/sync/status")
    suspend fun getSyncStatus(): Response<Map<String, Any>>
}
