package com.ganesh.hisabkitabpro.domain.ai

object AiMemoryManager {

    private val memory = AiMemory()

    fun recordCommand(command: String) {

        memory.commandHistory.add(command)

        val count = memory.frequentActions[command] ?: 0
        memory.frequentActions[command] = count + 1
    }

    fun recordCustomerInteraction(customer: String) {

        val count = memory.customerPatterns[customer] ?: 0
        memory.customerPatterns[customer] = count + 1
    }

    fun getMostFrequentCommand(): String? {

        return memory.frequentActions
            .maxByOrNull { it.value }
            ?.key
    }

    fun getMostActiveCustomer(): String? {

        return memory.customerPatterns
            .maxByOrNull { it.value }
            ?.key
    }
}