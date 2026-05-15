package com.ganesh.hisabkitabpro.network

import android.content.Context
import com.ganesh.hisabkitabpro.BuildConfig
import com.ganesh.hisabkitabpro.R
import okhttp3.CertificatePinner
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.TlsVersion
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * HISABKITAB PRO — HARDENED NETWORK ENGINE
 *
 * Call [install] once from [com.ganesh.hisabkitabpro.HisabKitabApp.onCreate] before any Retrofit use.
 * When [BuildConfig.CERT_PINNING_ENABLED] is true and real `sha256/…` pins are present in
 * [R.array.cert_pin_sha256_pins] (no PLACEHOLDER text), OkHttp applies [CertificatePinner].
 */
object RetrofitClient {

    private var appContext: Context? = null

    fun install(context: Context) {
        if (appContext != null) return
        synchronized(this) {
            if (appContext != null) return
            appContext = context.applicationContext
        }
    }

    private fun ctx(): Context =
        appContext ?: error("RetrofitClient.install(Application) must run in Application.onCreate")

    @Volatile
    private var authToken: String? = null

    /**
     * Sets the Bearer token for backend calls. Blank or whitespace clears the header
     * (same as logged-out) so we never send `Bearer ` with an empty secret.
     */
    fun setToken(token: String) {
        authToken = token.takeIf { it.isNotBlank() }
    }

    fun currentToken(): String? = authToken

    private val authInterceptor = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        authToken?.let { builder.addHeader("Authorization", "Bearer $it") }
        chain.proceed(builder.build())
    }

    private val gzipInterceptor = GzipRequestInterceptor()

    private val tlsConnectionSpec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
        .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
        .build()

    private fun buildCertificatePinner(): CertificatePinner? {
        if (!BuildConfig.CERT_PINNING_ENABLED) return null
        val context = ctx()
        val host = context.getString(R.string.cert_pin_hostname)
        val pins = context.resources.getStringArray(R.array.cert_pin_sha256_pins)
            .asSequence()
            .map { it.trim() }
            .filter {
                it.startsWith("sha256/") && !it.contains("PLACEHOLDER", ignoreCase = true)
            }
            .toList()
        if (pins.isEmpty()) return null
        val b = CertificatePinner.Builder()
        pins.forEach { pin -> b.add(host, pin) }
        return b.build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }
        val b = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
            .addInterceptor(gzipInterceptor)
            .connectionSpecs(listOf(tlsConnectionSpec))
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
        buildCertificatePinner()?.let { b.certificatePinner(it) }
        b.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(NetworkConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)
}

internal class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original: Request = chain.request()
        val body = original.body
        if (body == null ||
            original.header("Content-Encoding") != null ||
            (body.contentLength() in 0..GZIP_MIN_BYTES)
        ) {
            return chain.proceed(original)
        }
        val compressed = original.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(original.method, gzip(body))
            .build()
        return chain.proceed(compressed)
    }

    private fun gzip(body: RequestBody): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = body.contentType()
        override fun contentLength(): Long = -1
        override fun writeTo(sink: BufferedSink) {
            val gzipSink = GzipSink(sink).buffer()
            body.writeTo(gzipSink)
            gzipSink.close()
        }
    }

    companion object {
        private const val GZIP_MIN_BYTES = 512L
    }
}
