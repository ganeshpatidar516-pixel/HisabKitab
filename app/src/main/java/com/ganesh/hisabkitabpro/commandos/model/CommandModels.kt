package com.ganesh.hisabkitabpro.commandos.model

enum class IntentName {
    LEDGER_ADD,
    BILL_CLEAR,
    REMINDER_SEND,
    CUSTOMER_ADD,
    OPEN_SCREEN,
    SETTING_UPDATE,
    /** Read-only questions answered from local ledger/customer data (no writes). */
    LEDGER_QUERY,
    UNKNOWN
}

enum class TransactionClass {
    HARD_ATOMIC,
    FINANCIAL_ATOMIC_NOTIFICATION_EVENTUAL
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class ResolvedEntities(
    val customerName: String? = null,
    val customerPhone: String? = null,
    val targetRoute: String? = null,
    val amount: Long? = null,
    val settingKey: String? = null,
    val settingValue: String? = null
)

data class CommandIntent(
    val name: IntentName,
    val confidence: Double,
    val templateId: String
)

data class ExecutionPolicy(
    val transactionClass: TransactionClass,
    val riskLevel: RiskLevel,
    val requiresConfirmation: Boolean
)

data class ParsedCommand(
    val rawInput: String,
    val normalizedInput: String,
    val locale: String,
    val intent: CommandIntent,
    val entities: ResolvedEntities,
    val policy: ExecutionPolicy
)

sealed class CommandResult {
    data class Success(val message: String) : CommandResult()
    data class ClarificationRequired(val question: String) : CommandResult()
    data class Rejected(val reason: String) : CommandResult()
}
