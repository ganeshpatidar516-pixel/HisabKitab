package com.ganesh.hisabkitabpro.domain.invoice

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateRegistryPdfNormalizeTest {

    @Test
    fun normalizePdfTemplateId_modernFamily_mapsToGlass() {
        assertEquals("TEMP_GLASS", TemplateRegistry.normalizePdfTemplateId("TEMP_CYBER"))
        assertEquals("TEMP_GLASS", TemplateRegistry.normalizePdfTemplateId("TEMP_ROYAL"))
    }

    @Test
    fun normalizePdfTemplateId_standardFamily_mapsToSimple() {
        assertEquals("TEMP_SIMPLE", TemplateRegistry.normalizePdfTemplateId("TEMP_GST_STD"))
        assertEquals("TEMP_SIMPLE", TemplateRegistry.normalizePdfTemplateId(null))
    }

    @Test
    fun billPdfPickerTemplates_areCuratedSubset() {
        val picker = TemplateRegistry.getBillPdfPickerTemplates()
        assertEquals(5, picker.size)
        assert(picker.any { it.id == "TEMP_SIMPLE" })
        assert(picker.any { it.id == "TEMP_GLASS" })
    }
}
