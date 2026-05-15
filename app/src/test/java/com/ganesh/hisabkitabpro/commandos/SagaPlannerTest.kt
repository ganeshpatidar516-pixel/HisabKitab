package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.model.TransactionClass
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaActionType
import com.ganesh.hisabkitabpro.commandos.orchestrator.saga.SagaPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SagaPlannerTest {
    @Test
    fun planner_buildsExpectedCompoundFlow() {
        val planner = SagaPlanner()
        val request = planner.planCompoundCommand(
            customerName = "ramesh",
            amount = 500,
            includeBillClear = true,
            includeReminder = true
        )

        assertEquals(TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL, request.transactionClass)
        assertEquals(3, request.steps.size)
        assertEquals(SagaActionType.LEDGER_ADD, request.steps[0].action)
        assertEquals(SagaActionType.BILL_CLEAR, request.steps[1].action)
        assertEquals(SagaActionType.REMINDER_SEND, request.steps[2].action)
        assertTrue(request.steps.all { it.customerName == "ramesh" })
    }
}
