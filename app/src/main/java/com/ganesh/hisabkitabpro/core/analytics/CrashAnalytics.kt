package com.ganesh.hisabkitabpro.core.analytics

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsProvider {
    fun logException(throwable: Throwable)
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
}

@Singleton
class CrashAnalytics @Inject constructor() : AnalyticsProvider {

    override fun logException(throwable: Throwable) {
        // In a real app, this would be:
        // FirebaseCrashlytics.getInstance().recordException(throwable)
        // Sentry.captureException(throwable)
        Log.e("CrashAnalytics", "Exception recorded", throwable)
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        // FirebaseAnalytics.getInstance(context).logEvent(name, bundle)
        Log.i("CrashAnalytics", "Event: $name, ParamCount: ${params.size}")
    }
}
