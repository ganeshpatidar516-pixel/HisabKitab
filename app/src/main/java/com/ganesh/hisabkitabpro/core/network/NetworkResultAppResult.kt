package com.ganesh.hisabkitabpro.core.network

import com.ganesh.hisabkitabpro.core.common.AppError
import com.ganesh.hisabkitabpro.core.common.AppResult

/** Maps existing [NetworkResult] to Tier-C [AppResult] without changing call sites yet. */
fun <T> NetworkResult<T>.toAppResult(): AppResult<T> = when (this) {
    is NetworkResult.Success -> AppResult.Ok(data)
    is NetworkResult.RateLimitReached -> AppResult.Err(AppError.RateLimited)
    is NetworkResult.NetworkError -> AppResult.Err(AppError.Network)
    is NetworkResult.Error -> {
        when (code) {
            401, 403 -> AppResult.Err(AppError.Auth)
            else -> AppResult.Err(AppError.Http(code ?: -1, message))
        }
    }
}

fun <T> AppResult<T>.toKotlinResult(): Result<T> = when (this) {
    is AppResult.Ok -> Result.success(value)
    is AppResult.Err -> Result.failure(
        when (val e = error) {
            is AppError.Http -> Exception("HTTP ${e.code}: ${e.message}")
            is AppError.Auth -> Exception("Authentication required")
            is AppError.Network -> Exception("Network unavailable")
            is AppError.RateLimited -> Exception("Too many requests")
            is AppError.Unknown -> Exception(e.message ?: "Unknown error")
        },
    )
}
