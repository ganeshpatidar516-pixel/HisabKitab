package com.ganesh.hisabkitabpro.domain.ai

/**
 * HISABKITAB PRO - 🧠 AI RESPONSE MODEL
 * Advanced communication model for AI-User interaction.
 */
data class AIResponse(
    val message: String,
    val type: ResponseType = ResponseType.TEXT,
    val actions: List<AIAction> = emptyList(),
    val data: Map<String, String> = emptyMap(),
    val success: Boolean = true
)

enum class ResponseType {
    TEXT, 
    CUSTOMER_CARD, 
    TRANSACTION_CARD, 
    INVOICE_CARD, 
    ADVERTISEMENT,
    KNOWLEDGE_BASE, // 📚 External Wiki/Search data
    SYSTEM_STATUS    // 🛠️ Recovery/Safe mode alerts
}

data class AIAction(
    val label: String,
    val route: String,
    val icon: String? = null
)
