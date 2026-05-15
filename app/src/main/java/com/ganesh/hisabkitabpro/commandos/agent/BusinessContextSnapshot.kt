package com.ganesh.hisabkitabpro.commandos.agent

/**
 * Read-only business snapshot for future agent / LLM context. Built only from existing repositories.
 */
data class CustomerBalanceLine(
    val name: String,
    val balancePaise: Long
)

data class BusinessContextSnapshot(
    val builtAtEpochMillis: Long,
    val businessName: String?,
    val activeCustomerCount: Int,
    /** Same source as dashboard: SUM(customers.balanceCache) for non-deleted customers. */
    val overallNetBalancePaise: Long,
    val topPositiveBalances: List<CustomerBalanceLine>,
    /** Sorted sample of names (helps fuzzy command resolution hints; cap in builder). */
    val sampleCustomerNames: List<String>
)

/**
 * Compact block for logs or a future on-device / cloud model prompt (no PII beyond names + totals).
 */
fun BusinessContextSnapshot.toCompactPrompt(): String = buildString {
    appendLine("[HisabKitab business context]")
    appendLine("generatedAt=${builtAtEpochMillis}")
    appendLine("businessName=${businessName ?: "unknown"}")
    appendLine("activeCustomers=$activeCustomerCount")
    appendLine("overallNetBalancePaise=$overallNetBalancePaise")
    if (topPositiveBalances.isNotEmpty()) {
        appendLine("topReceivableRupeeHint:")
        topPositiveBalances.forEach { line ->
            appendLine("  - ${line.name}: ${line.balancePaise} paise")
        }
    }
    if (sampleCustomerNames.isNotEmpty()) {
        appendLine("customerNameSample=${sampleCustomerNames.joinToString(", ")}")
    }
}
