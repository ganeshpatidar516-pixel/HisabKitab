package com.ganesh.hisabkitabpro.core.common

/**
 * Unified repository / use-case outcome (Tier C).
 * Adopt incrementally — sacred ledger paths may keep [kotlin.Result] until explicitly migrated.
 */
sealed class AppResult<out T> {
    data class Ok<T>(val value: T) : AppResult<T>()
    data class Err(val error: AppError) : AppResult<Nothing>()

    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    fun getOrNull(): T? = (this as? Ok)?.value

    inline fun <R> map(transform: (T) -> R): AppResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    companion object {
        fun <T> ok(value: T): AppResult<T> = Ok(value)
        fun err(error: AppError): AppResult<Nothing> = Err(error)
        fun <T> fromResult(result: Result<T>): AppResult<T> = result.fold(
            onSuccess = { Ok(it) },
            onFailure = { Err(AppError.Unknown(it.message)) },
        )
    }
}

sealed class AppError {
    data object Network : AppError()
    data object Auth : AppError()
    data object RateLimited : AppError()
    data class Http(val code: Int, val message: String? = null) : AppError()
    data class Unknown(val message: String? = null) : AppError()
}
