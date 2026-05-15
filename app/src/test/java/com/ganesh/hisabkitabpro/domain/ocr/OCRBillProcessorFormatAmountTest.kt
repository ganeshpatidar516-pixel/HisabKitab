package com.ganesh.hisabkitabpro.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

/** Wave 4 — pure keypad formatting (no DI / ML Kit). */
class OCRBillProcessorFormatAmountTest {

    @Test
    fun wholeRupees_noDecimalSuffix() {
        assertEquals("42", OCRBillProcessor.formatAmountForKeypad(42.0))
        assertEquals("100", OCRBillProcessor.formatAmountForKeypad(100.0))
    }

    @Test
    fun twoDecimals_trimmedTrailingZeros() {
        assertEquals("10.5", OCRBillProcessor.formatAmountForKeypad(10.5))
        assertEquals("3.14", OCRBillProcessor.formatAmountForKeypad(3.14))
    }

    @Test
    fun roundsHalfUpToWholeRupeesWhenNeeded() {
        assertEquals("100", OCRBillProcessor.formatAmountForKeypad(99.995))
    }
}
