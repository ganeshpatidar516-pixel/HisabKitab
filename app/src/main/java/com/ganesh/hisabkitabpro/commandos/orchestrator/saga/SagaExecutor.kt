package com.ganesh.hisabkitabpro.commandos.orchestrator.saga

import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.model.TransactionClass

class SagaExecutor(
    private val adapter: DomainAdapter
) {
    suspend fun execute(request: SagaExecutionRequest): SagaExecutionResult {
        val results = mutableListOf<SagaStepResult>()
        val executed = mutableListOf<SagaStep>()

        for (step in request.steps) {
            val result = executeStep(step)
            results.add(result)

            if (result.success) {
                executed.add(step)
                continue
            }

            if (request.transactionClass == TransactionClass.FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL &&
                step.action == SagaActionType.REMINDER_SEND
            ) {
                // Notification failures do not break financial commit.
                continue
            }

            val rollbackSafe = rollbackExecuted(executed)
            return SagaExecutionResult(
                success = false,
                results = results,
                requiresManualReview = !rollbackSafe
            )
        }

        return SagaExecutionResult(success = true, results = results)
    }

    private suspend fun executeStep(step: SagaStep): SagaStepResult {
        return when (step.action) {
            SagaActionType.LEDGER_ADD -> {
                val ok = !step.customerName.isNullOrBlank() && step.amount != null &&
                    adapter.addLedgerEntry(step.customerName, step.amount)
                SagaStepResult(step, ok, if (ok) "Ledger add ok" else "Ledger add failed")
            }
            SagaActionType.BILL_CLEAR -> {
                val ok = !step.customerName.isNullOrBlank() && adapter.clearBill(step.customerName)
                SagaStepResult(step, ok, if (ok) "Bill clear ok" else "Bill clear failed")
            }
            SagaActionType.REMINDER_SEND -> {
                val ok = !step.customerName.isNullOrBlank() && adapter.sendReminder(step.customerName)
                SagaStepResult(step, ok, if (ok) "Reminder sent" else "Reminder failed")
            }
            SagaActionType.SETTING_UPDATE -> {
                val ok = !step.settingKey.isNullOrBlank() &&
                    !step.settingValue.isNullOrBlank() &&
                    adapter.updateSetting(step.settingKey, step.settingValue)
                SagaStepResult(step, ok, if (ok) "Setting updated" else "Setting update failed")
            }
        }
    }

    private suspend fun rollbackExecuted(executedSteps: List<SagaStep>): Boolean {
        // Safe default: financial operations are not auto-reversed without explicit domain support.
        // Returning false flags manual review to protect ledger integrity.
        if (executedSteps.any { it.action == SagaActionType.LEDGER_ADD || it.action == SagaActionType.BILL_CLEAR }) {
            return false
        }
        return true
    }
}
