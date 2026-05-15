package com.ganesh.hisabkitabpro.domain.model

/**
 * [Block 6: Data Layer] - Standard Transaction Types.
 * Single Source of Truth based on Blueprint.
 */
enum class TransactionType {
    CREDIT,    // Udhaar (Money Given)
    DEBIT,     // Jama (Money Received)
    INVOICE,   // Professional Bill Generated
    PAYMENT,   // Explicit Payment against invoice/ledger
    ADJUSTMENT // Manual balance correction
}
