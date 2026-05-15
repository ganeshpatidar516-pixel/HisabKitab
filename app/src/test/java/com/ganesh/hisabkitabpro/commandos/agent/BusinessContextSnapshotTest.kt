package com.ganesh.hisabkitabpro.commandos.agent

import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessContextSnapshotTest {
    @Test
    fun toCompactPrompt_containsKeyLines() {
        val s = BusinessContextSnapshot(
            builtAtEpochMillis = 1L,
            businessName = "Test Biz",
            activeCustomerCount = 2,
            overallNetBalancePaise = 500L,
            topPositiveBalances = listOf(CustomerBalanceLine("Ram", 500L)),
            sampleCustomerNames = listOf("Ram", "Shyam")
        )
        val t = s.toCompactPrompt()
        assertTrue(t.contains("Test Biz"))
        assertTrue(t.contains("overallNetBalancePaise=500"))
        assertTrue(t.contains("Ram"))
    }
}
