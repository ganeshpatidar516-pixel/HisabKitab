package com.ganesh.hisabkitabpro.domain.ai

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.CUSTOMER_AI_SNAPSHOT_LIMIT
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import com.ganesh.hisabkitabpro.network.RetrofitClient
import com.ganesh.hisabkitabpro.network.api.AiAssistantApi
import com.ganesh.hisabkitabpro.network.api.ChatRequest
import com.ganesh.hisabkitabpro.network.api.WikipediaApi
import com.ganesh.hisabkitabpro.ui.viewmodel.TransactionViewModel
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

/**
 * HISABKITAB PRO - 🧠 ADVANCED AI BRAIN (GEMINI/CHATGPT ARCHITECTURE)
 * High-resilience command routing with proactive error shielding.
 */
class AICommandRouter(
    private val viewModel: TransactionViewModel,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository?,
    private val context: Context
) {
    private val wikiApi by lazy { RetrofitClient.retrofit.create(WikipediaApi::class.java) }
    private val backendAiApi by lazy { RetrofitClient.retrofit.create(AiAssistantApi::class.java) }

    private var pendingAmount: Long? = null
    private var pendingType: TransactionType? = null

    suspend fun processCommand(command: String): AIResponse {
        return try {
            safeProcess(command)
        } catch (e: Throwable) {
            Log.e("AIRouter", "🛡️ AI Brain Shielded from crash: ${e.localizedMessage}")
            AIResponse(message = "मालिक, नेटवर्क या इंटरनल एरर की वजह से मैं अभी प्रोसेस नहीं कर पाया। कृपया दोबारा कोशिश करें।")
        }
    }

    private suspend fun safeProcess(command: String): AIResponse {
        val lower = command.lowercase(Locale.getDefault())
        
        // 1. Marketing Intent Detection
        if (isMarketingCommand(lower)) {
            return handleMarketingIntent(command)
        }

        // 2. Ledger Follow-up Logic
        if (pendingAmount != null && !isTransactionCommand(lower)) {
            val matchedCustomer = findBestMatch(command, customersForMatching(command))
            if (matchedCustomer != null) {
                return executePendingTransaction(matchedCustomer)
            }
        }

        // 3. Main Transaction Engine
        if (isTransactionCommand(lower)) {
            return handleNewTransaction(command)
        }

        // 4. External Knowledge Retrieval
        if (lower.contains("kya hai") || lower.contains("what is")) {
            return searchExternalKnowledge(command)
        }

        // 5. Cloud-Native AI Logic (Hybrid Mode)
        return try {
            val response = backendAiApi.chat(ChatRequest(command))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                mapBackendResponse(body)
            } else {
                getLocalBusinessResponse(lower)
            }
        } catch (e: Exception) {
            getLocalBusinessResponse(lower)
        }
    }

    private fun mapBackendResponse(body: com.ganesh.hisabkitabpro.network.api.ChatResponse): AIResponse {
        return when (body.actionType) {
            "MARKETING_IMAGE", "MARKETING_VIDEO" -> {
                AIResponse(
                    message = body.response,
                    type = ResponseType.ADVERTISEMENT,
                    actions = listOf(AIAction("Open Marketing Hub", "marketing_hub")),
                    data = body.payload
                )
            }
            "SEARCH" -> AIResponse(message = body.response, type = ResponseType.KNOWLEDGE_BASE)
            else -> AIResponse(message = body.response)
        }
    }

    private fun isMarketingCommand(cmd: String): Boolean {
        val keywords = listOf("poster", "ad", "marketing", "video", "promo", "विज्ञापन", "एड", "बनाओ", "डिजाइन", "बैनर", "banner")
        return keywords.any { cmd.contains(it) }
    }

    private suspend fun handleMarketingIntent(command: String): AIResponse {
        return AIResponse(
            message = "मालिक, आपका डिजाइन तैयार किया जा रहा है... Marketing Hub में देखें।",
            type = ResponseType.ADVERTISEMENT,
            actions = listOf(AIAction("Open Hub", "marketing_hub"))
        )
    }

    private fun isTransactionCommand(cmd: String): Boolean {
        val keywords = listOf("उधार", "add", "credit", "likho", "जमा", "paid", "debit", "mil gaye", "received", "given", "rupay", "rupees", "rs")
        return keywords.any { cmd.contains(it) } || "\\d+".toRegex().containsMatchIn(cmd)
    }

    private suspend fun handleNewTransaction(command: String): AIResponse {
        val amountRupees = extractAmount(command)
        val amountPaise = (amountRupees * 100).toLong()
        val type = if (command.contains("जमा") || command.contains("paid") || command.contains("received") || command.contains("mil gaye")) 
                      TransactionType.DEBIT else TransactionType.CREDIT
        
        val customer = findBestMatch(command, customersForMatching(command))

        return if (customer != null && amountPaise > 0) {
            clearPending()
            viewModel.addTransaction(
                customerId = customer.id,
                amountPaise = amountPaise,
                type = type,
                note = "AI Voice: $command"
            )
            AIResponse(
                message = "✅ हुक्म की तामील! ₹$amountRupees ${if(type == TransactionType.DEBIT) "जमा" else "उधार"} कर दिया है ${customer.name} के खाते में।",
                type = ResponseType.TRANSACTION_CARD,
                actions = listOf(AIAction("खाता देखें", "customer_ledger/${customer.id}"))
            )
        } else if (amountPaise > 0) {
            pendingAmount = amountPaise
            pendingType = type
            AIResponse(message = "मालिक, ₹$amountRupees तो मिल गया पर किसके खाते में? कृपया ग्राहक का नाम बताएं।")
        } else {
            AIResponse(message = "मालिक, अमाउंट समझ नहीं आया। जैसे: 'अजय 500 जमा'.")
        }
    }

    private suspend fun executePendingTransaction(customer: com.ganesh.hisabkitabpro.domain.model.Customer): AIResponse {
        val amtPaise = pendingAmount ?: 0L
        val type = pendingType ?: TransactionType.CREDIT
        viewModel.addTransaction(
            customerId = customer.id,
            amountPaise = amtPaise,
            type = type,
            note = "AI Brain Match"
        )
        val amtRupees = amtPaise / 100.0
        clearPending()
        return AIResponse(
            message = "✅ डन! ₹$amtRupees ${if(type == TransactionType.DEBIT) "जमा" else "उधार"} कर दिया गया है ${customer.name} के खाते में।",
            type = ResponseType.TRANSACTION_CARD,
            actions = listOf(AIAction("खाता देखें", "customer_ledger/${customer.id}"))
        )
    }

    private suspend fun customersForMatching(text: String): List<com.ganesh.hisabkitabpro.domain.model.Customer> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()
        val seen = linkedSetOf<Long>()
        val out = mutableListOf<com.ganesh.hisabkitabpro.domain.model.Customer>()
        val tokens = cleaned.split(Regex("\\s+")).filter { it.length >= 2 }.take(4)
        val queries = (tokens + cleaned).distinct()
        for (q in queries) {
            customerRepository.searchCustomers(q).forEach { c ->
                if (seen.add(c.id)) {
                    out.add(c)
                    if (out.size >= CUSTOMER_AI_SNAPSHOT_LIMIT) return out
                }
            }
        }
        if (out.isEmpty()) {
            out.addAll(customerRepository.getTopDebtorsLimited(50))
        }
        return out.take(CUSTOMER_AI_SNAPSHOT_LIMIT)
    }

    private fun findBestMatch(input: String, customers: List<com.ganesh.hisabkitabpro.domain.model.Customer>): com.ganesh.hisabkitabpro.domain.model.Customer? {
        if (customers.isEmpty()) return null
        val cleanedInput = input.lowercase(Locale.getDefault()).replace("ग्राहक का नाम", "").trim()
        
        // Exact or contains match
        customers.find { cleanedInput.contains(it.name.lowercase()) || it.name.lowercase().contains(cleanedInput) }?.let { return it }
        
        // Fuzzy match for resilience
        return customers.minByOrNull { levenshtein(cleanedInput, it.name.lowercase()) }?.let { 
            if (levenshtein(cleanedInput, it.name.lowercase()) < 4) it else null
        }
    }

    private fun extractAmount(cmd: String): Double {
        val match = "(\\d+)".toRegex().find(cmd)
        return match?.value?.toDoubleOrNull() ?: 0.0
    }

    private fun clearPending() {
        pendingAmount = null
        pendingType = null
    }

    private fun getLocalBusinessResponse(query: String): AIResponse {
        return when {
            query.contains("hii") || query.contains("hello") || query.contains("namaste") -> 
                AIResponse(message = "नमस्ते मालिक! मैं HisabKitab AI हूँ। मैं हिसाब लिखने, पोस्टर बनाने और बिजनेस सलाह देने में माहिर हूँ।")
            query.contains("kaise ho") || query.contains("how are you") ->
                AIResponse(message = "मैं एकदम शानदार हूँ मालिक! आप बताएं, आज क्या हिसाब लिखना है?")
            else -> AIResponse(message = "मालिक, मैं आपकी बात पूरी तरह समझ नहीं पाया। हिसाब के लिए 'नाम 500 जमा' बोलें।")
        }
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    private suspend fun searchExternalKnowledge(query: String): AIResponse {
        return try {
            val searchTerm = query.replace("kya hai", "").replace("what is", "").trim()
            if (searchTerm.isEmpty()) return AIResponse(message = "मालिक, किसके बारे में जानना चाहते हैं?")
            
            val response = wikiApi.searchKnowledge(titles = searchTerm)
            val extract = response.query.pages.values.firstOrNull()?.extract
            AIResponse(message = extract ?: "क्षमा करें मालिक, मुझे इसकी जानकारी नहीं मिली। आप ChatGPT मोड ऑन कर सकते हैं।")
        } catch (e: Exception) {
            AIResponse(message = "ज्ञान केंद्र (Server) अभी ऑफलाइन है।")
        }
    }
}
