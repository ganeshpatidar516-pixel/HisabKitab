package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class DialectRegistryTest {
    private val registry = DialectRegistry()

    @Test
    fun apply_mapsKotaDialectPhrasesToCanonical() {
        val output = registry.apply("ramesh ko yaad dila", "hinglish-hi")
        assertEquals("ramesh ko send reminder", output)
    }

    @Test
    fun apply_keepsInputUnchangedWhenLocalePackMissing() {
        val output = registry.apply("ramesh ko yaad dila", "en")
        assertEquals("ramesh ko yaad dila", output)
    }

    @Test
    fun apply_mapsNavigationPhrasesToCanonical() {
        val output = registry.apply("settings kholo", "hinglish-hi")
        assertEquals("open settings", output)
    }
}
