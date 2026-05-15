package com.ganesh.hisabkitabpro.commandos

import android.content.SharedPreferences
import com.ganesh.hisabkitabpro.commandos.sync.QueueHealthMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueHealthMetricsTest {
    @Test
    fun metrics_recordAndSnapshot_roundTrip() {
        val prefs = InMemoryPrefs()
        val metrics = QueueHealthMetrics(prefs)

        metrics.record(pendingCount = 7, processedCount = 3, status = "PARTIAL")
        val snapshot = metrics.snapshot()

        assertEquals(7, snapshot.pendingCount)
        assertEquals(3, snapshot.lastProcessedCount)
        assertEquals("PARTIAL", snapshot.lastStatus)
    }

    private class InMemoryPrefs : SharedPreferences {
        private val data = mutableMapOf<String, Any>()
        override fun getAll(): MutableMap<String, *> = data
        override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
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
            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null && value != null) updates[key] = value
                return this
            }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) updates[key] = null
                return this
            }
            override fun clear(): SharedPreferences.Editor {
                data.clear()
                updates.clear()
                return this
            }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                updates.forEach { (k, v) ->
                    if (v == null) data.remove(k) else data[k] = v
                }
                updates.clear()
            }
        }
    }
}
