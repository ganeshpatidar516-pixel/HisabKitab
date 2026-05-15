package com.ganesh.hisabkitabpro.core.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object RateLimiter {
    private val requestCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val lastResetTime = ConcurrentHashMap<String, Long>()
    
    private const val MAX_REQUESTS_PER_MINUTE = 60
    private const val ONE_MINUTE_MS = 60_000L

    fun shouldAllowRequest(endpoint: String): Boolean {
        val now = System.currentTimeMillis()
        val lastReset = lastResetTime.getOrDefault(endpoint, 0L)
        
        if (now - lastReset > ONE_MINUTE_MS) {
            requestCounts[endpoint] = AtomicInteger(0)
            lastResetTime[endpoint] = now
        }

        val count = requestCounts.getOrPut(endpoint) { AtomicInteger(0) }.incrementAndGet()
        return count <= MAX_REQUESTS_PER_MINUTE
    }
}
