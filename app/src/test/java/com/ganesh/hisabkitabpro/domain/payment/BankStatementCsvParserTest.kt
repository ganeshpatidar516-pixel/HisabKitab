package com.ganesh.hisabkitabpro.domain.payment

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankStatementCsvParserTest {

    @Test
    fun parse_supports_single_amount_column() {
        val csv = """
            Date,Description,Amount,Reference
            06/05/2026,UPI CR from customer,1500.00,UTR1234
            06/05/2026,UPI DR to vendor,-200.00,UTR9999
        """.trimIndent()

        val rows = BankStatementCsvParser.parse(csv).getOrThrow()
        assertEquals(2, rows.size)
        assertEquals(150000L, rows[0].amountPaise)
        assertEquals(-20000L, rows[1].amountPaise)
        assertEquals("UTR1234", rows[0].reference)
    }

    @Test
    fun parse_supports_credit_debit_columns() {
        val csv = """
            Transaction Date,Narration,Credit,Debit,UTR
            06/05/2026,UPI collect,300.00,,ABC1
            07/05/2026,ATM withdrawal,,100.00,ABC2
        """.trimIndent()

        val rows = BankStatementCsvParser.parse(csv).getOrThrow()
        assertEquals(2, rows.size)
        assertEquals(30000L, rows[0].amountPaise)
        assertEquals(-10000L, rows[1].amountPaise)
    }

    @Test
    fun reconciliation_proposes_high_confidence_for_exact_amount() {
        val row = BankStatementCsvParser.StatementRow(
            postedAtRaw = "06/05/2026",
            description = "UPI receive",
            amountPaise = 125000L,
            reference = "R1",
            rowNumber = 2
        )
        val ledger = listOf(
            Transaction(
                id = 99,
                customerId = 10,
                amount = 125000L,
                type = TransactionType.DEBIT,
                createdAt = System.currentTimeMillis()
            )
        )

        val candidates = BankStatementReconciliationEngine.proposeMatches(
            statementRows = listOf(row),
            ledgerTransactions = ledger
        )
        val best = candidates.firstOrNull()
        assertNotNull(best)
        assertNotNull(best?.transaction)
        assertTrue((best?.confidence ?: 0) >= 75)
    }
}
