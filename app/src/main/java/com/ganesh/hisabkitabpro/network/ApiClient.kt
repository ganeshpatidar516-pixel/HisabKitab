package com.ganesh.hisabkitabpro.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @deprecated Use [RetrofitClient] (auth, TLS, pinning, 401 refresh). This client has no security interceptors.
 */
@Deprecated(
    message = "Use RetrofitClient via Hilt — no auth/TLS hardening",
    level = DeprecationLevel.ERROR,
)
object ApiClient {
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Deprecated(
        message = "Use RetrofitClient via Hilt",
        level = DeprecationLevel.ERROR,
    )
    fun <T> createService(serviceClass: Class<T>): T {
        error("ApiClient is disabled — inject APIs from RetrofitClient (Hilt AppModule)")
    }
}