package com.ganesh.hisabkitabpro.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

/** Wave 6 — confidence / source metadata (numeric path must match [OCRExtractor.extractAmount]). */
class OCRExtractorDetailTest {

    @Test
    fun detailMatchesExtractAmount_grandTotal() {
        val text = "Grand Total 500.00"
        val d = OCRExtractor.extractAmountDetail(text)
        assertEquals(OCRExtractor.extractAmount(text), d.rupees, 0.001)
        assertEquals(500.0, d.rupees, 0.001)
        assertEquals(BillAmountConfidence.HIGH, d.confidence)
        assertEquals(BillAmountSource.KEYWORD_SAME_LINE, d.source)
    }

    @Test
    fun keywordFollowLine_medium() {
        val text = """
            taxes
            Total
            250.00
        """.trimIndent()
        val d = OCRExtractor.extractAmountDetail(text)
        assertEquals(OCRExtractor.extractAmount(text), d.rupees, 0.001)
        assertEquals(250.0, d.rupees, 0.001)
        assertEquals(BillAmountConfidence.MEDIUM, d.confidence)
        assertEquals(BillAmountSource.KEYWORD_FOLLOW_LINE, d.source)
    }

    @Test
    fun blank_none() {
        val d = OCRExtractor.extractAmountDetail("")
        assertEquals(0.0, d.rupees, 0.001)
        assertEquals(BillAmountSource.NONE, d.source)
        assertEquals(BillAmountConfidence.LOW, d.confidence)
    }

    @Test
    fun globalMax_whenFooterSliceHasNoDigits() {
        val head = (1..7).joinToString("\n") { "item $it  ${it * 2}.00" }
        val text = """
            $head
            notes only
            no numbers here
            still nothing
        """.trimIndent()
        val d = OCRExtractor.extractAmountDetail(text)
        assertEquals(OCRExtractor.extractAmount(text), d.rupees, 0.001)
        assertEquals(14.0, d.rupees, 0.001)
        assertEquals(BillAmountConfidence.LOW, d.confidence)
        assertEquals(BillAmountSource.GLOBAL_MAX, d.source)
    }

    @Test
    fun shortDoc_footerLow() {
        val text = "x 10\ny 25"
        val d = OCRExtractor.extractAmountDetail(text)
        assertEquals(25.0, d.rupees, 0.001)
        assertEquals(BillAmountConfidence.LOW, d.confidence)
        assertEquals(BillAmountSource.FOOTER_MAX, d.source)
    }
}
