package com.ganesh.hisabkitabpro.domain.ai.intelligence

import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.firstOrNull

class AIKnowledgeEngine(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository
) {

    suspend fun query(userQuery: String): AIResponse {
        // Priority 1: Search Inside Application Data
        val internalResult = searchInternalData(userQuery)
        if (internalResult != null) {
            return internalResult
        }

        // Priority 2: External Search (Simulated for this module)
        return fetchExternalKnowledge(userQuery)
    }

    private suspend fun searchInternalData(query: String): AIResponse? {
        val lowerQuery = query.lowercase()
        
        // Example: Check for balance of a specific customer
        if (lowerQuery.contains("balance") || lowerQuery.contains("hisab")) {
            val customers = customerRepository.getAllCustomers().firstOrNull() ?: emptyList()
            val target = customers.find { lowerQuery.contains(it.name.lowercase()) }
            
            if (target != null) {
                return AIResponse(
                    answer = "${target.name} का वर्तमान बैलेंस ₹${target.balanceCache / 100.0} है।",
                    source = "internal",
                    confidence = 0.98,
                    analysisType = "business"
                )
            }
        }
        return null
    }

    private fun fetchExternalKnowledge(query: String): AIResponse {
        // In a real app, this would call a search API or Gemini/OpenAI
        return AIResponse(
            answer = "I couldn't find this in your business records, but based on general business knowledge, standard credit period is 30 days.",
            source = "external",
            confidence = 0.75,
            analysisType = "knowledge"
        )
    }
}
