package com.ganesh.hisabkitabpro.domain.ledger

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("DEPRECATION")
class LedgerEngineTest {

    @Test
    fun runningBalance_usesPaise_andSkipsDeleted() {
        val entries = LedgerEngine.calculateRunningBalance(
            listOf(
                Transaction(
                    id = 1L,
                    customerId = 1L,
                    amount = 10_000L,
                    type = TransactionType.CREDIT,
                    createdAt = 1L,
                ),
                Transaction(
                    id = 2L,
                    customerId = 1L,
                    amount = 3_000L,
                    type = TransactionType.PAYMENT,
                    createdAt = 2L,
                ),
                Transaction(
                    id = 3L,
                    customerId = 1L,
                    amount = 99_000L,
                    type = TransactionType.CREDIT,
                    isDeleted = true,
                    createdAt = 3L,
                ),
            ),
        )
        assertEquals(2, entries.size)
        assertEquals(7_000L, entries.first().runningBalancePaise)
        assertEquals(10_000L, entries.last().runningBalancePaise)
    }
}
