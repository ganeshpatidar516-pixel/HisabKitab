package com.ganesh.hisabkitabpro.commandos.agent

import com.ganesh.hisabkitabpro.domain.repository.CUSTOMER_AI_SNAPSHOT_LIMIT
import com.ganesh.hisabkitabpro.domain.repository.CustomerRepository
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Aggregates read-only customer / balance context from existing data layers.
 * Does not write; safe to call from assistant / future agent planner.
 */
@Singleton
class BusinessContextBuilder @Inject constructor(
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend fun buildSnapshot(
        maxTopDebtors: Int = 5,
        maxNameSamples: Int = 30
    ): BusinessContextSnapshot {
        val overallNet = customerRepository.getOverallNetBalancePaise().firstOrNull() ?: 0L
        val activeCount = customerRepository.getCustomerCount().firstOrNull() ?: 0
        val profile = settingsRepository.getBusinessProfile().firstOrNull()
        val businessName = profile?.businessName?.trim()?.takeIf { it.isNotBlank() }

        val topDebtors = customerRepository.getTopDebtorsLimited(
            maxTopDebtors.coerceIn(0, CUSTOMER_AI_SNAPSHOT_LIMIT),
        )
        val top = topDebtors
            .map { CustomerBalanceLine(name = it.name, balancePaise = it.balanceCache) }

        val names = customerRepository.getCustomerNamesLimited(
            maxNameSamples.coerceIn(0, CUSTOMER_AI_SNAPSHOT_LIMIT),
        ).sortedWith(compareBy { it.lowercase(Locale.ROOT) })

        return BusinessContextSnapshot(
            builtAtEpochMillis = System.currentTimeMillis(),
            businessName = businessName,
            activeCustomerCount = activeCount,
            overallNetBalancePaise = overallNet,
            topPositiveBalances = top,
            sampleCustomerNames = names
        )
    }
}
