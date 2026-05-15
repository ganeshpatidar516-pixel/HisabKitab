package com.ganesh.hisabkitabpro.commandos.agent

import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentToolRegistryTest {
    @Test
    fun executeSuperCommand_delegatesToOrchestrator() = runBlocking {
        val adapter = object : DomainAdapter {
            override suspend fun searchCustomer(name: String): Boolean = true
            override suspend fun addCustomer(name: String, phone: String): Boolean = true
            override suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean = true
            override suspend fun clearBill(customerName: String): Boolean = true
            override suspend fun sendReminder(customerName: String): Boolean = true
            override suspend fun updateSetting(key: String, value: String): Boolean = true
        }
        val orchestrator = SuperCommandOrchestrator(
            normalizer = InputNormalizer(),
            dialectRegistry = DialectRegistry(),
            parser = DeterministicIntentParser(),
            policyGuard = PolicyGuard(),
            adapter = adapter
        )
        val registry = AgentToolRegistry(orchestrator)
        val result = registry.executeSuperCommand(AgentToolContext("ramesh ko 500 add karo"))
        assertTrue(result is CommandResult.Success)
    }

    @Test
    fun toAgentToolExecutionReport_mapsOutcomes() {
        assertEquals(
            "SUCCESS",
            CommandResult.Success("ok").toAgentToolExecutionReport().outcome
        )
        assertEquals(
            "CLARIFY",
            CommandResult.ClarificationRequired("?").toAgentToolExecutionReport().outcome
        )
        assertEquals(
            "REJECTED",
            CommandResult.Rejected("x").toAgentToolExecutionReport().outcome
        )
    }
}
