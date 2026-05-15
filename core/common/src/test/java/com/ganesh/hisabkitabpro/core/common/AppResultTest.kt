package com.ganesh.hisabkitabpro.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun map_ok_transformsValue() {
        val r = AppResult.ok(2).map { it * 3 }
        assertEquals(6, (r as AppResult.Ok).value)
    }

    @Test
    fun fromResult_failure_becomesErr() {
        val r = AppResult.fromResult(Result.failure<Int>(IllegalStateException("x")))
        assertTrue(r is AppResult.Err)
    }
}
