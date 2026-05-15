package com.ganesh.hisabkitabpro.network.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(
    val message: String,
    val merchant_id: String = "default_merchant"
)

/**
 * 🚀 GOD-MODE AI RESPONSE MODEL
 */
data class ChatResponse(
    @SerializedName("response") val response: String,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("payload") val payload: Map<String, String> = emptyMap()
)

interface AiAssistantApi {
    @POST("api/v1/ai/god-mode")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}
