package com.ganesh.hisabkitabpro.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryScanNormalizerTest {

    @Test
    fun keepsPlainBarcode() {
        assertEquals("8901234567890", InventoryScanNormalizer.normalize(" 8901234567890 "))
    }

    @Test
    fun extractsBarcodeFromKeyValueQr() {
        assertEquals(
            "8901234567890",
            InventoryScanNormalizer.normalize("name=Rice; barcode=8901234567890; price=90")
        )
    }

    @Test
    fun extractsSkuFromQr() {
        assertEquals("SKU-ATTA-01", InventoryScanNormalizer.normalize("sku: SKU-ATTA-01"))
    }

    @Test
    fun stripsUrlSchemeForStableLookup() {
        assertEquals("example.com/p/ABC123", InventoryScanNormalizer.normalize("https://example.com/p/ABC123"))
    }
}
