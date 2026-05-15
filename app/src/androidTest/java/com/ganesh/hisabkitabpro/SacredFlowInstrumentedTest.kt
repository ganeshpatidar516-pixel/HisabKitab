package com.ganesh.hisabkitabpro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.local.InvoiceEntity
import com.ganesh.hisabkitabpro.domain.model.Bill
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.payment.TransactionMatcher
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sacred-flow data contract tests (Room + balance helpers). No UI harness required.
 * These guard customer ledger math paths used by production flows.
 */
@RunWith(AndroidJUnit4::class)
class SacredFlowInstrumentedTest {

    @Test
    fun test_CreateCustomerAndAddEntry() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(ctx)
        val suffix = System.currentTimeMillis().toString().takeLast(8)
        val phone = "98$suffix"
        val customerId = db.customerDao().insertCustomer(
            Customer(name = "SacredTest Customer", phone = phone)
        )
        val creditPaise = 50_000L
        db.transactionDao().insertTransactionWithBalanceUpdate(
            Transaction(
                customerId = customerId,
                amount = creditPaise,
                type = TransactionType.CREDIT
            )
        )
        val refreshed = db.customerDao().getCustomerById(customerId)
        assertNotNull(refreshed)
        assertEquals(creditPaise, refreshed!!.balanceCache)
    }

    @Test
    fun test_GenerateBillAndCalculateGST() = runBlocking {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val db = AppDatabase.getDatabase(ctx)
        val suffix = System.currentTimeMillis().toString().takeLast(8)
        val customerId = db.customerDao().insertCustomer(
            Customer(name = "GST Bill Customer", phone = "97$suffix")
        )
        val subtotal = 100.0
        val gstRate = 18.0
        val gstAmount = subtotal * gstRate / 100.0
        val total = subtotal + gstAmount
        val invoiceId = "inv-sacred-${UUID.randomUUID()}"
        db.invoiceDao().insertInvoice(
            InvoiceEntity(
                invoiceId = invoiceId,
                customerId = customerId.toString(),
                customerName = "GST Bill Customer",
                subtotal = subtotal,
                gstEnabled = true,
                gstRate = gstRate,
                gstAmount = gstAmount,
                totalAmount = total
            )
        )
        val loaded = db.invoiceDao().getInvoiceById(invoiceId)
        assertNotNull(loaded)
        assertTrue(loaded!!.gstEnabled)
        assertEquals(18.0, loaded.gstAmount, 0.001)
        assertEquals(118.0, loaded.totalAmount, 0.001)
    }

    @Test
    fun test_BankSmsParserExtractsAmountAndLastFour() {
        val text = "Your a/c XX5678 is credited with Rs.1,250.00 on 01-May-26"
        val parsed = TransactionMatcher.parseSms("Bank Alert", text)
        assertNotNull(parsed)
        assertEquals(125_000L, parsed!!.amountPaise)
        assertEquals("5678", parsed.accountLastFour)
    }
}
