package com.ganesh.hisabkitabpro.commandos.orchestrator.saga

import com.ganesh.hisabkitabpro.commandos.model.TransactionClass

class SagaPlanner {
    fun planCompoundCommand(
        customerName: String,
        amount: Long?,
        includeBillClear: Boolean,
        includeReminder: Boolean
    ): SagaExecutionRequest {
        val steps = mutableListOf<SagaStep>()
        if (amount != null) {
            steps.add(SagaStep(action = SagaActionType.LEDGER_ADD, customerName = customerName, amount = amount))
        }
        if (includeBillClear) {
            steps.add(SagaStep(action = SagaActionType.BILL_CLEAR, customerName = customerName))
        }
        if (includeReminder) {
            steps.add(SagaStep(action = SagaActionType.REMINDER_SEND, customerName = customerName))
        }
        return SagaExecutionRequest(
            steps = steps,
            transactionClass = TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL
        )
    }
}
