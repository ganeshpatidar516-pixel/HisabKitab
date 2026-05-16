package com.ganesh.hisabkitabpro.core.firebase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Method

class OpsTelemetryHubTest {

    @Test
    fun sanitize_blocksPiiLikeKeys() {
        val sanitize = sanitizeMethod()
        val out = sanitize.invoke(
            OpsTelemetryHub,
            mapOf(
                "outcome" to "ok",
                "customer_name" to "secret",
                "amount_rupee" to "500",
                "len_bucket" to "42",
            ),
        ) as Map<*, *>
        assertTrue(out.containsKey("outcome"))
        assertFalse(out.containsKey("customer_name"))
        assertFalse(out.containsKey("amount_rupee"))
        assertTrue(out.containsKey("len_bucket"))
    }

    @Test
    fun sanitize_truncatesLongValues() {
        val sanitize = sanitizeMethod()
        val long = "x".repeat(200)
        val out = sanitize.invoke(
            OpsTelemetryHub,
            mapOf("code" to long),
        ) as Map<*, *>
        assertEquals(100, (out["code"] as String).length)
    }

    private fun sanitizeMethod(): Method {
        val clazz = OpsTelemetryHub::class.java
        val m = clazz.getDeclaredMethod(
            "sanitize",
            Map::class.java,
        )
        m.isAccessible = true
        return m
    }
}
