package com.ganesh.hisabkitabpro.commandos.agent

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
        val customers = customerRepository.getAllCustomers()
            .firstOrNull()
            .orEmpty()
            .filter { !it.isDeleted }

        val overallNet = customerRepository.getOverallNetBalancePaise().firstOrNull() ?: 0L
        val profile = settingsRepository.getBusinessProfile().firstOrNull()
        val businessName = profile?.businessName?.trim()?.takeIf { it.isNotBlank() }

        val top = customers
            .asSequence()
            .filter { it.balanceCache > 0L }
            .sortedByDescending { it.balanceCache }
            .take(maxTopDebtors.coerceAtLeast(0))
            .map { CustomerBalanceLine(name = it.name, balancePaise = it.balanceCache) }
            .toList()

        val names = customers
            .map { it.name }
            .sortedWith(compareBy { it.lowercase(Locale.ROOT) })
            .take(maxNameSamples.coerceAtLeast(0))

        return BusinessContextSnapshot(
            builtAtEpochMillis = System.currentTimeMillis(),
            businessName = businessName,
            activeCustomerCount = customers.size,
            overallNetBalancePaise = overallNet,
            topPositiveBalances = top,
            sampleCustomerNames = names
        )
    }
}
