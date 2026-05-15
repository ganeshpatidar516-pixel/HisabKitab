package com.ganesh.hisabkitabpro.domain.ocr

/** Wave 6 — coarse signal for QA / logcat; never log raw OCR text here. */
enum class BillAmountConfidence {
    /** Keyword line produced the first plausible amount token. */
    HIGH,
    /** Amount on a line following a keyword, or footer slice with enough lines to be meaningful. */
    MEDIUM,
    /** Whole-document footer, global max fallback, or no amount. */
    LOW,
}

enum class BillAmountSource {
    KEYWORD_SAME_LINE,
    KEYWORD_FOLLOW_LINE,
    FOOTER_MAX,
    GLOBAL_MAX,
    NONE,
}

data class BillAmountExtraction(
    val rupees: Double,
    val confidence: BillAmountConfidence,
    val source: BillAmountSource,
)
