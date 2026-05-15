package com.ganesh.hisabkitabpro.commandos

import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.commandos.adapters.contracts.DomainAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.agent.AgentToolRegistry
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandSimulationTest {
    @Test
    fun simulation_normalLedgerCommand_executesSuccessfully() = runBlocking {
        val adapter = FakeDomainAdapter()
        val service = serviceWith(adapter = adapter, enabled = true)

        val result = service.run("ramesh ko 500 add karo")

        assertTrue(result is CommandResult.Success)
        assertEquals(1, adapter.ledgerAdds)
    }

    @Test
    fun simulation_ambiguousCommand_isRejected() = runBlocking {
        val adapter = FakeDomainAdapter()
        val service = serviceWith(adapter = adapter, enabled = true)

        val result = service.run("kuch random command")

        assertTrue(result is CommandResult.Rejected)
        assertEquals(0, adapter.ledgerAdds + adapter.billClears + adapter.reminders)
    }

    @Test
    fun simulation_highRiskBillClear_requiresConfirmationBeforeExecution() = runBlocking {
        val adapter = FakeDomainAdapter()
        val service = serviceWith(adapter = adapter, enabled = true)

        val firstAttempt = service.run("ramesh ka bill clear karo")
        val confirmedAttempt = service.run("ramesh ka bill clear karo", userConfirmed = true)

        assertTrue(firstAttempt is CommandResult.ClarificationRequired)
        assertTrue(confirmedAttempt is CommandResult.Success)
        assertEquals(1, adapter.billClears)
    }

    @Test
    fun simulation_financialCommand_withoutUserConfirmed_neverExecutes() = runBlocking {
        val adapter = FakeDomainAdapter()
        val service = serviceWith(adapter = adapter, enabled = true)

        val first = service.run("ramesh ka bill clear karo")
        val second = service.run("ramesh ka bill clear karo")

        assertTrue(first is CommandResult.ClarificationRequired)
        assertTrue(second is CommandResult.ClarificationRequired)
        assertEquals(0, adapter.billClears)
    }

    @Test
    fun simulation_hindiLedgerCommand_executesSuccessfully() = runBlocking {
        val adapter = FakeDomainAdapter()
        val service = serviceWith(adapter = adapter, enabled = true)

        val result = service.run("गणेश को ₹500 ऐड करो")

        assertTrue(result is CommandResult.Success)
        assertEquals(1, adapter.ledgerAdds)
    }

    @Test
    fun simulation_aggregateBalanceQuery_usesInsightAdapter() = runBlocking {
        val adapter = FakeDomainAdapter().apply {
            stubInsight = "📒 test snapshot"
        }
        val service = serviceWith(adapter = adapter, enabled = true)

        val result = service.run("sabka balance kitna hai")

        assertTrue(result is CommandResult.Success)
        assertEquals("📒 test snapshot", (result as CommandResult.Success).message)
    }

    @Test
    fun simulation_unknownCustomerLedger_doesNotPost_andShowsHints() = runBlocking {
        val adapter = FakeDomainAdapter().apply {
            customerFound = false
            stubSuggestions = listOf("Ramesh", "Suresh")
        }
        val service = serviceWith(adapter = adapter, enabled = true)

        val result = service.run("wrongname ko 500 add karo")

        assertTrue(result is CommandResult.Success)
        val msg = (result as CommandResult.Success).message
        assertTrue(msg.contains("wrongname"))
        assertTrue(msg.contains("Ramesh"))
        assertEquals(0, adapter.ledgerAdds)
    }

    private fun serviceWith(adapter: DomainAdapter, enabled: Boolean): SuperCommandService {
        val prefs = InMemoryPrefs()
        val toggle = SuperCommandFeatureToggle(prefs)
        toggle.setEnabled(enabled)
        val orchestrator = SuperCommandOrchestrator(
            normalizer = InputNormalizer(),
            dialectRegistry = DialectRegistry(),
            parser = DeterministicIntentParser(),
            policyGuard = PolicyGuard(),
            adapter = adapter
        )
        return SuperCommandService(toggle = toggle, agentToolRegistry = AgentToolRegistry(orchestrator))
    }

    private class FakeDomainAdapter : DomainAdapter {
        var ledgerAdds = 0
        var billClears = 0
        var reminders = 0
        var customerAdds = 0
        var customerFound: Boolean = true
        var stubSuggestions: List<String> = emptyList()
        var stubInsight: String? = null

        override suspend fun searchCustomer(name: String): Boolean = customerFound

        override suspend fun suggestCustomerNames(query: String, limit: Int): List<String> =
            stubSuggestions.take(limit)

        override suspend fun answerLedgerInsightQuery(
            normalizedInput: String,
            customerHint: String?
        ): String? = stubInsight

        override suspend fun addCustomer(name: String, phone: String): Boolean {
            customerAdds += 1
            return true
        }

        override suspend fun addLedgerEntry(customerName: String, amount: Long): Boolean {
            ledgerAdds += 1
            return true
        }

        override suspend fun clearBill(customerName: String): Boolean {
            billClears += 1
            return true
        }

        override suspend fun sendReminder(customerName: String): Boolean {
            reminders += 1
            return true
        }

        override suspend fun updateSetting(key: String, value: String): Boolean = true
    }

    private class InMemoryPrefs : SharedPreferences {
        private val data = mutableMapOf<String, Any>()

        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            val value = data[key]
            return when (value) {
                is Set<*> -> value.filterIsInstance<String>().toMutableSet()
                else -> defValues
            }
        }
        override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = key != null && data.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor(data)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(private val data: MutableMap<String, Any>) : SharedPreferences.Editor {
            private val updates = mutableMapOf<String, Any?>()
            private var clear = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) updates[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
                if (key != null && values != null) updates[key] = values
                return this
            }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) updates[key] = null
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                clear = true
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clear) data.clear()
                updates.forEach { (k, v) ->
                    if (v == null) data.remove(k) else data[k] = v
                }
                updates.clear()
                clear = false
            }
        }
    }
}
