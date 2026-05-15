package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class InputNormalizerTest {
    private val normalizer = InputNormalizer()

    @Test
    fun normalize_convertsSimpleCompositeNumberWord() {
        val output = normalizer.normalize("Ramesh ko paanch sau add karo")
        assertEquals("ramesh ko 500 add karo", output)
    }

    @Test
    fun normalize_stripsPunctuationAndExtraSpaces() {
        val output = normalizer.normalize("  Ramesh!!   ko   500 add karo  ")
        assertEquals("ramesh ko 500 add karo", output)
    }

    @Test
    fun normalize_fixesCommonTyposAndRepeats() {
        val output = normalizer.normalize("Ramesh ko 500 ad kro kro")
        assertEquals("ramesh ko 500 add karo", output)
    }

    @Test
    fun normalize_mapsPhoneticBillAndReminderWords() {
        val output = normalizer.normalize("Ganesh ka hishab clear kro yad bhejo")
        assertEquals("ganesh ka bill clear karo reminder bhejo", output)
    }

    @Test
    fun normalize_mapsKhateMeinAndRupeeToLedgerShape() {
        val output = normalizer.normalize("Ganesh ke khate mein ₹500 jodo")
        assertEquals("ganesh ke account me 500 add", output)
    }
}
