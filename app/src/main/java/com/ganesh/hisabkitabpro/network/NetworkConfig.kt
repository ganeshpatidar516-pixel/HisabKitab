package com.ganesh.hisabkitabpro.network

import com.ganesh.hisabkitabpro.BuildConfig

object NetworkConfig {
    /** Override via `api.base.url` in local.properties (dev/staging); defaults to production Railway. */
    val BASE_URL: String = BuildConfig.BASE_URL
}
