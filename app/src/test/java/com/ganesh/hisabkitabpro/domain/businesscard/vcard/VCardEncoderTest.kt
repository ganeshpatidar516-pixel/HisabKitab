package com.ganesh.hisabkitabpro.domain.businesscard.vcard

import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardEncoderTest {

    @Test
    fun `encoded payload starts and ends with vcard markers`() {
        val payload = VCardEncoder.encode(sampleProfile())
        assertTrue(payload.startsWith("BEGIN:VCARD\r\nVERSION:3.0"))
        assertTrue(payload.trim().endsWith("END:VCARD"))
    }

    @Test
    fun `phone email org and address are encoded`() {
        val payload = VCardEncoder.encode(sampleProfile())
        assertTrue(payload.contains("FN:Ganesh Patidar"))
        assertTrue(payload.contains("ORG:HisabKitab Pro"))
        assertTrue(payload.contains("TEL;TYPE=CELL,VOICE:9991112222"))
        assertTrue(payload.contains("EMAIL;TYPE=INTERNET:owner@example.com"))
        assertTrue(payload.contains("ADR;TYPE=WORK"))
        assertTrue(payload.contains("Indore"))
    }

    @Test
    fun `empty profile falls back to business name`() {
        val payload = VCardEncoder.encode(
            BusinessProfile(
                businessName = "Solo Studio",
                ownerName = "",
                phone = "",
                email = "",
                address = "",
            ),
        )
        assertTrue(payload.contains("FN:Solo Studio"))
        assertFalse(payload.contains("TEL"))
        assertFalse(payload.contains("EMAIL"))
    }

    @Test
    fun `commas semicolons and backslashes are escaped`() {
        val payload = VCardEncoder.encode(
            BusinessProfile(
                businessName = "A; Tricky, Name\\Co",
                ownerName = "Owner",
                phone = "12345",
            ),
        )
        assertTrue(payload.contains("ORG:A\\; Tricky\\, Name\\\\Co"))
    }

    @Test
    fun `notes line is omitted when no metadata is present`() {
        val payload = VCardEncoder.encode(
            BusinessProfile(
                businessName = "X",
                ownerName = "Y",
                phone = "1",
                email = "y@x.com",
                address = "Earth",
                gstNumber = "",
                panNumber = "",
                upiId = "",
            ),
        )
        assertFalse(payload.contains("NOTE:"))
    }

    @Test
    fun `notes line carries gstin pan and upi when present`() {
        val payload = VCardEncoder.encode(sampleProfile())
        assertTrue(payload.contains("NOTE:"))
        assertTrue(payload.contains("GSTIN 22AAAAA0000A1Z5"))
        assertTrue(payload.contains("PAN AAAAA0000A"))
        assertTrue(payload.contains("UPI ganesh@upi"))
    }

    @Test
    fun `notes include tagline services and cta when present`() {
        val payload = VCardEncoder.encode(
            sampleProfile().copy(
                tagline = "Smart billing",
                servicesDescription = "Invoicing\nReminders",
                cardCtaText = "Try Pro today",
            ),
        )
        assertTrue(payload.contains("NOTE:"))
        assertTrue(payload.contains("Smart billing"))
        assertTrue(payload.contains("Services"))
        assertTrue(payload.contains("Invoicing"))
        assertTrue(payload.contains("Try Pro today"))
    }

    private fun sampleProfile(): BusinessProfile = BusinessProfile(
        businessName = "HisabKitab Pro",
        ownerName = "Ganesh Patidar",
        address = "MG Road, Indore",
        phone = "9991112222",
        email = "owner@example.com",
        gstNumber = "22AAAAA0000A1Z5",
        panNumber = "AAAAA0000A",
        upiId = "ganesh@upi",
    )
}
