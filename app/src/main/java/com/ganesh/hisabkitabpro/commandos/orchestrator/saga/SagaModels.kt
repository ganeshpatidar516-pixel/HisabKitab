package com.ganesh.hisabkitabpro.commandos.orchestrator.saga

import com.ganesh.hisabkitabpro.commandos.model.TransactionClass

enum class SagaActionType {
    LEDGER_ADD,
    BILL_CLEAR,
    REMINDER_SEND,
    SETTING_UPDATE
}

data class SagaStep(
    val action: SagaActionType,
    val customerName: String? = null,
    val amount: Long? = null,
    val settingKey: String? = null,
    val settingValue: String? = null
)

data class SagaExecutionRequest(
    val steps: List<SagaStep>,
    val transactionClass: TransactionClass
)

data class SagaStepResult(
    val step: SagaStep,
    val success: Boolean,
    val message: String
)

data class SagaExecutionResult(
    val success: Boolean,
    val results: List<SagaStepResult>,
    val requiresManualReview: Boolean = false
)
