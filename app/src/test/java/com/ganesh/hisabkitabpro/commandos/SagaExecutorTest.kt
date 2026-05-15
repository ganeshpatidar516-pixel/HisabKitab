package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.model.TransactionClass
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaActionType
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaExecutionRequest
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaExecutor
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SagaExecutorTest {
    @Test
    fun financialAtomic_notificationFailureStillSucceeds() = runBlocking {
        val adapter = FakeAdapter(reminderOk = false)
        val executor = SagaExecutor(adapter)
        val req = SagaExecutionRequest(
            steps = listOf(
                SagaStep(SagaActionType.LEDGER_ADD, customerName = "ramesh", amount = 500),
                SagaStep(SagaActionType.REMINDER_SEND, customerName = "ramesh")
            ),
            transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL
        )

        val res = executor.execute(req)
        assertTrue(res.success)
    }

    @Test
    fun hardAtomic_financialFailureFailsAndRequiresManualReview() = runBlocking {
        val adapter = FakeAdapter(billClearOk = false)
        val executor = SagaExecutor(adapter)
        val req = SagaExecutionRequest(
            steps = listOf(
                SagaStep(SagaActionType.LEDGER_ADD, customerName = "ramesh", amount = 500),
                SagaStep(SagaActionType.BILL_CLEAR, customerName = "ramesh")
            ),
            transactionClass = TransactionClass.HARD_ATOMIC
        )

        val res = executor.execute(req)
        assertFalse(res.success)
        assertTrue(res.requiresManualReview)
    }

    private class FakeAdapter(
        private val billClearOk: Boolean = true,
        private val reminderOk: Boolean = true
    ) : DomainAdapter {
        override suspend fun searchCustomer(name: String): Boolean = true
        override suspend fun addCustomer(name: String, phone: String): Boolean = true
        override suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean = true
        override suspend fun clearBill(customerName: String): Boolean = billClearOk
        override suspend fun sendReminder(customerName: String): Boolean = reminderOk
        override suspend fun updateSetting(key: String, value: String): Boolean = true
    }
}
