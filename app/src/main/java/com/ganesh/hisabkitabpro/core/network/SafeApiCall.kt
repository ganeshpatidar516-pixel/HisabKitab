package com.ganesh.hisabkitabpro.core.network

import android.util.Log
import com.ganesh.hisabkitabpro.core.security.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int? = null, val message: String? = null) : NetworkResult<Nothing>()
    object NetworkError : NetworkResult<Nothing>()
    object RateLimitReached : NetworkResult<Nothing>()
}

/** Avoid persisting or logging raw API error bodies (may contain tokens or PII). */
private fun sanitizedHttpMessage(code: Int): String = "HTTP $code"

suspend fun <T> safeApiCall(
    endpoint: String,
    apiCall: suspend () -> T
): NetworkResult<T> {
    if (!RateLimiter.shouldAllowRequest(endpoint)) {
        return NetworkResult.RateLimitReached
    }

    return withContext(Dispatchers.IO) {
        try {
            val response = RetryEngine.retryWithExponentialBackoff {
                apiCall.invoke()
            }
            NetworkResult.Success(response)
        } catch (throwable: Throwable) {
            Log.w(
                "SafeApiCall",
                "API failed endpoint=$endpoint type=${throwable::class.java.simpleName}",
            )
            when (throwable) {
                is IOException -> NetworkResult.NetworkError
                is HttpException -> {
                    val code = throwable.code()
                    NetworkResult.Error(code, sanitizedHttpMessage(code))
                }
                else -> NetworkResult.Error(null, null)
            }
        }
    }
}
