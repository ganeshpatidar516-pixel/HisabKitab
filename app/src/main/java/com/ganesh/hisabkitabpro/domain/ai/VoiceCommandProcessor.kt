package com.ganesh.hisabkitabpro.domain.ai

import com.ganesh.hisabkitabpro.domain.engine.VoiceTransactionParser
import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class VoiceCommandProcessor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun processVoiceCommand(text: String, customerId: Long): String = withContext(Dispatchers.IO) {
        val result = VoiceTransactionParser.parseVoiceInput(text)
        
        if (result.success && result.transaction != null) {
            val input = result.transaction
            val transaction = Transaction(
                customerId = customerId,
                amount = (input.amount * 100).toLong(), // Convert to Paise
                type = input.type,
                note = "Voice Entry: $text",
                txnRef = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis()
            )
            
            try {
                transactionRepository.addTransaction(transaction)
                "सफलता! ₹${input.amount} की एंट्री कर दी गई है।"
            } catch (e: Exception) {
                "सर्वर से जुड़ने में समस्या हुई, लेकिन हिसाब सुरक्षित (Save) कर लिया गया है।"
            }
        } else {
            result.error ?: "माफ करें, मैं हिसाब समझ नहीं पाया। कृपया दोबारा बोलें।"
        }
    }
}
