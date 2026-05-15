package com.ganesh.hisabkitabpro.domain.ocr

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Wave 3–4 — golden / regression tests for on-device bill text heuristics (no ML Kit). */
class OCRExtractorTest {

    private lateinit var savedDefaultLocale: Locale

    @Before
    fun saveLocale() {
        savedDefaultLocale = Locale.getDefault()
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(savedDefaultLocale)
    }

    @Test
    fun extractAmount_blank_returnsZero() {
        assertEquals(0.0, OCRExtractor.extractAmount(""), 0.001)
        assertEquals(0.0, OCRExtractor.extractAmount("   \n\t  "), 0.001)
    }

    @Test
    fun extractAmount_keywordSameLine() {
        val text = """
            line items here
            Grand Total 1,234.56
            thank you
        """.trimIndent()
        assertEquals(1234.56, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_subtotalAlone_usesMediumConfidencePath() {
        val text = """
            line items
            subtotal 900.00
        """.trimIndent()
        val d = OCRExtractor.extractAmountDetail(text)
        assertEquals(900.0, d.rupees, 0.001)
        assertEquals(BillAmountConfidence.MEDIUM, d.confidence)
    }

    @Test
    fun extractAmount_prefersGrandTotalOverEarlierSubtotal() {
        val text = """
            items
            subtotal 900.00
            discount 100.00
            grand total 800.00
        """.trimIndent()
        assertEquals(800.0, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_doesNotTreatInternetAsNetKeyword() {
        val text = """
            internet handling 15.00
            amount due 120.50
        """.trimIndent()
        assertEquals(120.5, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_keywordNextLine() {
        val text = """
            taxes included
            Total
            450.00
        """.trimIndent()
        assertEquals(450.0, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_hindiKeywordLine() {
        val text = """
            दुकान नाम
            कुल 2,500.50
            धन्यवाद
        """.trimIndent()
        assertEquals(2500.5, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_footerMaxWhenNoKeywordMatch() {
        val text = """
            a1
            b2
            c3
            d4
            e5
            f6
            noise 42
            ref 199
            other 50
        """.trimIndent()
        assertEquals(199.0, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_globalMaxFallback() {
        val text = """
            x 10
            y 25
        """.trimIndent()
        assertEquals(25.0, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_amountDue() {
        val text = "Amount Due  88.99"
        assertEquals(88.99, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractName_skipsInvoiceHeader() {
        val text = """
            TAX INVOICE
            Coffee House MG Road
            Date: 01-01-2026
        """.trimIndent()
        assertEquals("Coffee House MG Road", OCRExtractor.extractName(text))
    }

    @Test
    fun extractName_empty_returnsUnknown() {
        assertEquals("Unknown Vendor", OCRExtractor.extractName(""))
        assertEquals("Unknown Vendor", OCRExtractor.extractName("\n  \n"))
    }

    @Test
    fun extractLineItemsSummary_dropsTotalLines() {
        val text = """
            Widget A  120.00
            Widget B  80.50
            Subtotal 200.50
            Grand Total 200.50
        """.trimIndent()
        val summary = OCRExtractor.extractLineItemsSummary(text, maxLines = 12)
        assertTrue(summary.contains("Widget A"))
        assertTrue(summary.contains("Widget B"))
        assertTrue("summary should omit total-ish lines", !summary.contains("Grand Total"))
        assertTrue("summary should omit total-ish lines", !summary.contains("Subtotal"))
    }

    @Test
    fun extractLineItemsSummary_blank_returnsEmpty() {
        assertEquals("", OCRExtractor.extractLineItemsSummary(""))
    }

    @Test
    fun extractAmount_indianGroupingWithDecimals() {
        val text = "Grand Total 12,34,567.89"
        assertEquals(1234567.89, OCRExtractor.extractAmount(text), 0.001)
    }

    @Test
    fun extractAmount_plainIntegerToken() {
        assertEquals(999.0, OCRExtractor.extractAmount("Net Payable 999"), 0.001)
    }

    @Test
    fun extractAmount_turkishDefaultLocale_stillMatchesTotalKeyword() {
        Locale.setDefault(Locale("tr", "TR"))
        assertEquals(100.0, OCRExtractor.extractAmount("TOTAL 100.00"), 0.001)
    }

    @Test
    fun extractLineItemsSummary_respectsMaxLines() {
        val body = (1..20).joinToString("\n") { n -> "Item$n  ${n * 10}.00" }
        val summary = OCRExtractor.extractLineItemsSummary(body, maxLines = 3)
        val count = summary.lines().filter { it.isNotBlank() }.size
        assertTrue("expected at most 3 lines, got $count: $summary", count <= 3)
    }

    @Test
    fun extractName_skipsGstinHeavyLine() {
        val text = """
            Tax Invoice
            GSTIN 29ABCDE1234F1Z5
            Annapurna Sweets
        """.trimIndent()
        assertEquals("Annapurna Sweets", OCRExtractor.extractName(text))
    }
}
