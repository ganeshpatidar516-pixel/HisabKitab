package com.ganesh.hisabkitabpro.feature.telemetry

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the persistence + default contract of [TelemetryFeatureToggle].
 *
 * Mirrors the in-memory SharedPreferences pattern from
 * `SuperCommandFeatureToggleTest` so this test runs as a fast JVM unit test
 * (no Robolectric, no instrumentation, no Firebase touched).
 */
class TelemetryFeatureToggleTest {

    @Test
    fun crashReporting_defaultsToEnabled() {
        val toggle = TelemetryFeatureToggle(InMemoryPrefs())
        assertTrue(
            "Crashlytics must be ON by default so existing installs see no behavior change at upgrade.",
            toggle.isCrashReportingEnabled()
        )
    }

    @Test
    fun analytics_defaultsToEnabled() {
        val toggle = TelemetryFeatureToggle(InMemoryPrefs())
        assertTrue(
            "Analytics must be ON by default so existing installs see no behavior change at upgrade.",
            toggle.isAnalyticsEnabled()
        )
    }

    @Test
    fun setCrashReportingDisabled_persists() {
        val prefs = InMemoryPrefs()
        val toggle = TelemetryFeatureToggle(prefs)

        toggle.setCrashReportingEnabled(false)
        assertFalse(toggle.isCrashReportingEnabled())

        val rebuiltToggle = TelemetryFeatureToggle(prefs)
        assertFalse(
            "Crash reporting opt-out must survive across instances (proxy for app restart).",
            rebuiltToggle.isCrashReportingEnabled()
        )
    }

    @Test
    fun setAnalyticsDisabled_persists() {
        val prefs = InMemoryPrefs()
        val toggle = TelemetryFeatureToggle(prefs)

        toggle.setAnalyticsEnabled(false)
        assertFalse(toggle.isAnalyticsEnabled())

        val rebuiltToggle = TelemetryFeatureToggle(prefs)
        assertFalse(
            "Analytics opt-out must survive across instances (proxy for app restart).",
            rebuiltToggle.isAnalyticsEnabled()
        )
    }

    @Test
    fun togglesAreIndependent() {
        val prefs = InMemoryPrefs()
        val toggle = TelemetryFeatureToggle(prefs)

        toggle.setCrashReportingEnabled(false)
        assertFalse(toggle.isCrashReportingEnabled())
        assertTrue(
            "Disabling Crashlytics must NOT disable Analytics — they are independent toggles.",
            toggle.isAnalyticsEnabled()
        )

        toggle.setAnalyticsEnabled(false)
        toggle.setCrashReportingEnabled(true)
        assertTrue(toggle.isCrashReportingEnabled())
        assertFalse(
            "Re-enabling Crashlytics must NOT re-enable Analytics.",
            toggle.isAnalyticsEnabled()
        )
    }

    @Test
    fun persistenceKeys_matchPlayDataSafetyDoc() {
        val prefs = InMemoryPrefs()
        val toggle = TelemetryFeatureToggle(prefs)

        toggle.setCrashReportingEnabled(false)
        toggle.setAnalyticsEnabled(false)

        assertEquals(
            "Doc-side promise (PLAY_DATA_SAFETY_FORM_DRAFT.md §2.12) is `feature_crashlytics_v1`.",
            false,
            prefs.getBoolean(TelemetryFeatureToggle.KEY_CRASHLYTICS_V1, true)
        )
        assertEquals(
            "Doc-side promise (PLAY_DATA_SAFETY_FORM_DRAFT.md §2.10) is `feature_analytics_v1`.",
            false,
            prefs.getBoolean(TelemetryFeatureToggle.KEY_ANALYTICS_V1, true)
        )
        assertEquals("feature_crashlytics_v1", TelemetryFeatureToggle.KEY_CRASHLYTICS_V1)
        assertEquals("feature_analytics_v1", TelemetryFeatureToggle.KEY_ANALYTICS_V1)
    }

    @Test
    fun toggleCanBeReEnabled_afterDisable() {
        val prefs = InMemoryPrefs()
        val toggle = TelemetryFeatureToggle(prefs)

        toggle.setCrashReportingEnabled(false)
        toggle.setAnalyticsEnabled(false)
        toggle.setCrashReportingEnabled(true)
        toggle.setAnalyticsEnabled(true)

        assertTrue(toggle.isCrashReportingEnabled())
        assertTrue(toggle.isAnalyticsEnabled())
    }

    /**
     * Minimal in-memory [SharedPreferences] for fast JVM-only unit testing.
     * Same shape as [com.ganesh.hisabkitabpro.commandos.SuperCommandFeatureToggleTest]'s helper —
     * factored locally to keep this test self-contained.
     */
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
        override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

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
                updates.forEach { (k, v) -> if (v == null) data.remove(k) else data[k] = v }
                updates.clear()
                clear = false
            }
        }
    }
}
