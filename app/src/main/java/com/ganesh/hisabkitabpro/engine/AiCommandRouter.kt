package com.ganesh.hisabkitabpro.engine

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.*

sealed class AiActionResult {
    data class Success(val message: String) : AiActionResult()
    data class Error(val message: String) : AiActionResult()
    object NavigateToMarketing : AiActionResult()
    data class NavigateToLedger(val customerId: Long) : AiActionResult()
}

class AiCommandRouter(
    private val transactionRepository: TransactionRepository,
    private val customerRepository: CustomerRepository
) {

    suspend fun routeCommand(command: String): AiActionResult {
        val lowerCommand = command.lowercase(Locale.getDefault())

        return try {
            when {
                // Marketing Intents
                lowerCommand.contains("ad") || 
                lowerCommand.contains("poster") || 
                lowerCommand.contains("video") || 
                lowerCommand.contains("marketing") ||
                lowerCommand.contains("promotion") -> {
                    AiActionResult.NavigateToMarketing
                }

                // Ledger Intents
                lowerCommand.contains("add") || 
                lowerCommand.contains("jama") || 
                lowerCommand.contains("उधार") ||
                lowerCommand.contains("paid") || 
                lowerCommand.contains("mil gaye") || 
                lowerCommand.contains("भुगतान") -> {
                    val type = if (lowerCommand.contains("add") || lowerCommand.contains("jama") || lowerCommand.contains("उधार")) {
                        TransactionType.CREDIT
                    } else {
                        TransactionType.DEBIT
                    }
                    handleTransactionCommand(command, type)
                }

                else -> AiActionResult.Error("I didn't understand. Try 'Create a marketing poster' or 'Add 500 credit to Ramesh'.")
            }
        } catch (e: Exception) {
            AiActionResult.Error("Error routing command: ${e.localizedMessage}")
        }
    }

    private suspend fun handleTransactionCommand(command: String, type: TransactionType): AiActionResult {
        return try {
            val amountRupees = "(\\d+)".toRegex().find(command)?.value?.toDoubleOrNull() 
                ?: return AiActionResult.Error("Amount not found.")
            val amountPaise = (amountRupees * 100).toLong()

            val words = command.split(" ")
            val name = words.lastOrNull() ?: "Unknown"

            val customers = customerRepository.getAllCustomers().firstOrNull()
            val customer = customers?.find { 
                it.name.equals(name, ignoreCase = true) 
            } ?: return AiActionResult.Error("Customer '$name' not found.")

            val transaction = Transaction(
                amount = amountPaise,
                type = type,
                customerId = customer.id,
                note = "Added via AI: $command",
                txnRef = UUID.randomUUID().toString()
            )

            transactionRepository.addTransaction(transaction)
            
            if (type == TransactionType.CREDIT) {
                customerRepository.updateCustomerBalance(customer.id, amountPaise, 0L)
            } else {
                customerRepository.updateCustomerBalance(customer.id, 0L, amountPaise)
            }

            val typeText = if (type == TransactionType.CREDIT) "Udhaar (Credit)" else "Payment (Debit)"
            AiActionResult.Success("Success! Added ₹$amountRupees $typeText for ${customer.name}.")
        } catch (e: Exception) {
            AiActionResult.Error("Crash prevented: ${e.localizedMessage ?: "Unknown error"}")
        }
    }
}
