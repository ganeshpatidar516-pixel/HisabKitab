package com.ganesh.hisabkitabpro.domain.businesscard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCardFieldRegistryTest {

    @Test
    fun orderedRailLines_matches_builder_contract() {
        val profile = BusinessCardProfile.EMPTY.copy(
            businessCategory = "Retail",
            phone = "1",
            email = "e@e.com",
            gstin = "G",
            pan = "P",
            upi = "u@ybl",
            instagramUrl = "https://instagram.com/x",
        )
        val fromRegistry = BusinessCardFieldRegistry.orderedRailLines(profile)
        val fromBuilder = BusinessCardContactLineBuilder.lines(profile)
        assertEquals(fromRegistry, fromBuilder)
        assertTrue(fromRegistry.size >= 6)
    }

    @Test
    fun orderedRailLines_includes_cta_and_service_lines() {
        val profile = BusinessCardProfile.EMPTY.copy(
            phone = "9000000000",
            cardCtaText = "Book today",
            servicesDescription = "Plumbing\nRepairs",
        )
        val lines = BusinessCardFieldRegistry.orderedRailLines(profile)
        assertTrue(lines.any { it.contains("◇") && it.contains("Book today") })
        assertTrue(lines.any { it.startsWith("·") && it.contains("Plumbing") })
        assertTrue(lines.any { it.contains("Repairs") })
    }

    @Test
    fun orderedRailLines_address_gst_services_before_social_urls() {
        val profile = BusinessCardProfile.EMPTY.copy(
            phone = "1",
            email = "e@e.com",
            address = "123 Main St",
            gstin = "GST123",
            instagramUrl = "https://instagram.com/brand",
            cardCtaText = "Visit us",
            servicesDescription = "Tea\nSnacks",
        )
        val lines = BusinessCardFieldRegistry.orderedRailLines(profile)
        val iAddr = lines.indexOfFirst { it.startsWith("A ") && it.contains("Main St") }
        val iGst = lines.indexOfFirst { it.startsWith("GSTIN") }
        val iIg = lines.indexOfFirst { it.startsWith("IG ") }
        val iSvc = lines.indexOfFirst { it.startsWith("·") && it.contains("Tea") }
        assertTrue(iAddr >= 0 && iGst >= 0 && iIg >= 0 && iSvc >= 0)
        assertTrue(iAddr < iIg)
        assertTrue(iGst < iIg)
        assertTrue(iIg < iSvc)
    }

    @Test
    fun orderedRailLines_extra_appended_last() {
        val profile = BusinessCardProfile.EMPTY.copy(phone = "9")
        val lines = BusinessCardFieldRegistry.orderedRailLines(profile, extraRailLines = listOf("Extra line"))
        assertEquals("Extra line", lines.last())
    }
}
