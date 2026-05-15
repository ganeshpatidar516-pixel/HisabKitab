package com.ganesh.hisabkitabpro.domain.ocr

import java.util.Locale

object OCRExtractor {

    private val numberTokenRegex = Regex("""(\d{1,7}(?:,\d{2,3})*(?:\.\d{2})?)""")

    /**
     * Wave 5 — order matters: longer / bill-final phrases first; bare [total] last with word-boundary
     * so "subtotal" / "internet" do not false-trigger.
     */
    /** Bill-final amounts — searched first (highest trust). */
    private val finalTotalKeywords = listOf(
        "grand total",
        "total amount",
        "amount due",
        "amount payable",
        "कुल रकम",
        "कुल",
        "राशि",
        "योग",
        "नकद",
        "जमा",
        "balance",
        "payable",
        "net",
        "total",
    )

    /** Intermediate totals — only when no final-total keyword matched (medium confidence). */
    private val subtotalKeywords = listOf("subtotal")

    /** English tokens that appear inside longer words if matched naively with [String.contains]. */
    private val totalKeywordWordBoundaryEn = setOf("total", "net")

    private fun extractByKeywords(
        lines: List<String>,
        keywords: List<String>,
        sameLineConfidence: BillAmountConfidence,
        followLineConfidence: BillAmountConfidence,
    ): BillAmountExtraction? {
        for (keyword in keywords) {
            val index = lines.indexOfFirst { lineMatchesTotalKeyword(it, keyword) }
            if (index == -1) continue
            val match = numberTokenRegex.find(lines[index])
            if (match != null) {
                val amt = parseSafeDouble(match.value)
                if (amt > 0) {
                    return BillAmountExtraction(
                        amt,
                        sameLineConfidence,
                        BillAmountSource.KEYWORD_SAME_LINE,
                    )
                }
            }
            for (offset in 1..2) {
                if (index + offset < lines.size) {
                    val nextMatch = numberTokenRegex.find(lines[index + offset])
                    if (nextMatch != null) {
                        val amt = parseSafeDouble(nextMatch.value)
                        if (amt > 0) {
                            return BillAmountExtraction(
                                amt,
                                followLineConfidence,
                                BillAmountSource.KEYWORD_FOLLOW_LINE,
                            )
                        }
                    }
                }
            }
        }
        return null
    }

    private fun lineMatchesTotalKeyword(line: String, keyword: String): Boolean {
        if (!line.contains(keyword)) return false
        if (keyword !in totalKeywordWordBoundaryEn) return true
        val i = line.indexOf(keyword)
        if (i < 0) return false
        val before = if (i > 0) line[i - 1] else ' '
        val after = if (i + keyword.length < line.length) line[i + keyword.length] else ' '
        return !before.isLetter() && !after.isLetter()
    }

    /**
     * ULTRA-PRO MAX OCR Extraction Logic
     * Extracts the most likely bill amount using multi-layered heuristic analysis.
     */
    fun extractAmount(text: String): Double {
        val d = extractAmountDetail(text)
        return if (d.rupees > 0.0) d.rupees else 0.0
    }

    /**
     * Same numeric choice as [extractAmount], plus **coarse** confidence/source for telemetry / future UX.
     */
    fun extractAmountDetail(text: String): BillAmountExtraction {
        if (text.isBlank()) {
            return BillAmountExtraction(0.0, BillAmountConfidence.LOW, BillAmountSource.NONE)
        }

        // Locale.ROOT so English keywords match regardless of device default (e.g. Turkish i/I rules).
        val lines = text.lines().map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }

        // Layer 1a: Final-total keywords (highest confidence)
        extractByKeywords(
            lines = lines,
            keywords = finalTotalKeywords,
            sameLineConfidence = BillAmountConfidence.HIGH,
            followLineConfidence = BillAmountConfidence.MEDIUM,
        )?.let { return it }

        // Layer 1b: Subtotal only when no grand/total keyword matched (P1 — avoid ₹900 vs ₹1416 bills)
        extractByKeywords(
            lines = lines,
            keywords = subtotalKeywords,
            sameLineConfidence = BillAmountConfidence.MEDIUM,
            followLineConfidence = BillAmountConfidence.LOW,
        )?.let { return it }

        // Layer 2: Footer Analysis (Totals are usually in the last 30% of lines)
        val footerLines = if (lines.size > 5) lines.takeLast((lines.size * 0.35).toInt()) else lines
        val footerNumbers = footerLines.flatMap { line ->
            numberTokenRegex.findAll(line).map { parseSafeDouble(it.value) }
        }.filter { it > 0 }

        if (footerNumbers.isNotEmpty()) {
            val max = footerNumbers.maxOrNull() ?: 0.0
            if (max > 0.0) {
                val conf = if (lines.size > 5) BillAmountConfidence.MEDIUM else BillAmountConfidence.LOW
                return BillAmountExtraction(max, conf, BillAmountSource.FOOTER_MAX)
            }
        }

        // Layer 3: Global Search (Fallback - Largest number found)
        val globalNumbers = lines.flatMap { line ->
            numberTokenRegex.findAll(line).map { parseSafeDouble(it.value) }
        }.filter { it > 0 }

        val g = globalNumbers.maxOrNull() ?: 0.0
        return if (g > 0.0) {
            BillAmountExtraction(g, BillAmountConfidence.LOW, BillAmountSource.GLOBAL_MAX)
        } else {
            BillAmountExtraction(0.0, BillAmountConfidence.LOW, BillAmountSource.NONE)
        }
    }

    /**
     * Extracts Business/Vendor name using top-header heuristics.
     */
    fun extractName(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return "Unknown Vendor"

        val skipList = listOf(
            "tax invoice", "bill", "invoice", "cash memo", "receipt", "welcome", 
            "gstin", "date", "no:", "address", "tel:", "mobile", "phone", "email"
        )
        
        // Headers are usually in the first 5-6 lines
        for (i in 0 until minOf(6, lines.size)) {
            val line = lines[i]
            // Skip common headers and lines that are purely numeric
            if (line.length > 3 && skipList.none { line.contains(it, ignoreCase = true) }) {
                if (line.any { it.isLetter() }) {
                    return line
                }
            }
        }

        return lines[0]
    }

    private fun parseSafeDouble(raw: String): Double {
        return try {
            raw.replace(",", "").toDouble()
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Pulls likely line-items from OCR text (table-ish lines with letters + numbers) for note / template hints.
     */
    fun extractLineItemsSummary(rawText: String, maxLines: Int = 12): String {
        if (rawText.isBlank()) return ""
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val picked = lines.filter { line ->
            line.any { it.isLetter() } &&
                numberTokenRegex.find(line) != null &&
                line.length in 4..120 &&
                (finalTotalKeywords + subtotalKeywords).none { kw ->
                    lineMatchesTotalKeyword(line.lowercase(Locale.ROOT), kw)
                }
        }.distinct().take(maxLines)
        return picked.joinToString("\n")
    }
}
