package com.ganesh.hisabkitabpro.commandos

import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.commandos.adapters.hisabkitab.SafeNoOpHisabKitabAdapter
import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.CommandResult
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import com.ganesh.hisabkitabpro.commandos.agent.AgentToolRegistry
import com.ganesh.hisabkitabpro.commandos.orchestrator.SuperCommandOrchestrator
import com.ganesh.hisabkitabpro.commandos.policy.PolicyGuard
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SuperCommandServiceTest {
    @Test
    fun run_rejectsWhenFeatureFlagDisabled() = runBlocking {
        val prefs = InMemoryPrefs()
        val toggle = SuperCommandFeatureToggle(prefs)
        toggle.setEnabled(false)
        val service = SuperCommandService(toggle, AgentToolRegistry(safeOrchestrator()))

        val result = service.run("ramesh ko 500 add karo")
        assertTrue(result is CommandResult.Rejected)
    }

    @Test
    fun run_routesWhenFeatureFlagEnabled() = runBlocking {
        val prefs = InMemoryPrefs()
        val toggle = SuperCommandFeatureToggle(prefs)
        toggle.setEnabled(true)
        val service = SuperCommandService(toggle, AgentToolRegistry(safeOrchestrator()))

        val result = service.run("ramesh ko 500 add karo")
        assertTrue(result is CommandResult.Success)
        assertTrue((result as CommandResult.Success).message.contains("ramesh", ignoreCase = true))
    }

    private fun safeOrchestrator(): SuperCommandOrchestrator {
        return SuperCommandOrchestrator(
            normalizer = InputNormalizer(),
            dialectRegistry = DialectRegistry(),
            parser = DeterministicIntentParser(),
            policyGuard = PolicyGuard(),
            adapter = SafeNoOpHisabKitabAdapter()
        )
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
