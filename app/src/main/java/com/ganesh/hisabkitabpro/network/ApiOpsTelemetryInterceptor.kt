package com.ganesh.hisabkitabpro.network

import android.content.Context
import com.ganesh.hisabkitabpro.core.firebase.ProductionOpsTelemetry
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Phase-9 — release-safe API observability. Records path class, status bucket, and latency
 * without request/response bodies or auth headers.
 */
internal class ApiOpsTelemetryInterceptor(
    private val appContext: Context,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val method = request.method
        val started = System.nanoTime()
        return try {
            val response = chain.proceed(request)
            val elapsedMs = (System.nanoTime() - started) / 1_000_000L
            ProductionOpsTelemetry.recordApiCall(
                appContext,
                method = method,
                path = path,
                httpCode = response.code,
                durationMs = elapsedMs,
            )
            response
        } catch (e: IOException) {
            val elapsedMs = (System.nanoTime() - started) / 1_000_000L
            ProductionOpsTelemetry.recordApiCall(
                appContext,
                method = method,
                path = path,
                httpCode = -1,
                durationMs = elapsedMs,
                errorKind = e.javaClass.simpleName,
            )
            throw e
        }
    }
}
