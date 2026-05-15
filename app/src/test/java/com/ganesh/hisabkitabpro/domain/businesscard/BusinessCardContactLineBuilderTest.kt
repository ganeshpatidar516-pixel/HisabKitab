package com.ganesh.hisabkitabpro.domain.businesscard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCardContactLineBuilderTest {

    @Test
    fun lines_include_tax_social_and_category() {
        val profile = BusinessCardProfile.EMPTY.copy(
            businessCategory = "Retail",
            phone = "9000000000",
            email = "a@b.co",
            gstin = "22AAAAA0000A1Z5",
            pan = "ABCDE1234F",
            upi = "shop@paytm",
            instagramUrl = "https://www.instagram.com/myshop",
            linkedInUrl = "https://linkedin.com/in/foo",
            twitterUrl = "https://twitter.com/handle",
            googleBusinessProfileUrl = "https://g.page/mybiz",
        )
        val lines = BusinessCardContactLineBuilder.lines(profile)
        assertTrue(lines.any { it.startsWith("Category") })
        assertTrue(lines.any { it.contains("GSTIN") })
        assertTrue(lines.any { it.contains("PAN") })
        assertTrue(lines.any { it.contains("UPI") })
        assertTrue(lines.any { it.startsWith("IG") })
        assertTrue(lines.any { it.startsWith("LI") })
        assertTrue(lines.any { it.startsWith("X") })
        assertTrue(lines.any { it.startsWith("GBP") })
    }

    @Test
    fun shortenUrl_strips_scheme_and_caps_length() {
        val s = BusinessCardUrlDisplay.shorten("https://www.example.com/very/long/path", maxLen = 20)
        assertEquals(20, s.length)
        assertTrue(s.endsWith("…"))
        assertTrue(!s.contains("https", ignoreCase = true))
    }
}
