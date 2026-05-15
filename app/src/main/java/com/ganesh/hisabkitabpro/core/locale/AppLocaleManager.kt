package com.ganesh.hisabkitabpro.core.locale

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object AppLocaleManager {
    private const val PREFS = "hisabkitab_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language_code"

    fun getSavedLanguageCode(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "en")
            .orEmpty()
            .ifBlank { "en" }
    }

    /**
     * Persists language synchronously ([commit]) so any immediate [Activity.recreate] reads the same value.
     * Async [apply] caused DB≠prefs races: recreate → attachBaseContext still saw old code → recreate loop / “refresh”.
     */
    fun saveLanguageCode(context: Context, languageCode: String): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalizeLanguageCode(languageCode))
            .commit()
    }

    fun applyPersistedLocale(application: Application) {
        val code = getSavedLanguageCode(application)
        updateApplicationLocale(application, code)
    }

    fun updateLocaleNow(context: Context, languageCode: String) {
        val app = context.applicationContext as? Application ?: return
        updateApplicationLocale(app, languageCode)
    }

    fun wrapContext(base: Context): Context {
        val code = getSavedLanguageCode(base)
        return createLocalizedContext(base, code)
    }

    private fun updateApplicationLocale(application: Application, languageCode: String) {
        val locale = resolveLocale(normalizeLanguageCode(languageCode))
        Locale.setDefault(locale)
        val resources = application.resources
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun createLocalizedContext(base: Context, languageCode: String): Context {
        val locale = resolveLocale(normalizeLanguageCode(languageCode))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return base.createConfigurationContext(config)
    }

    private fun resolveLocale(languageCode: String): Locale {
        return when (languageCode) {
            "system" -> Resources.getSystem().configuration.locales[0] ?: Locale.getDefault()
            "en" -> Locale.ENGLISH
            else -> Locale.forLanguageTag(languageCode)
        }
    }

    fun normalizeLanguageCode(languageCode: String): String {
        val normalized = mapLegacyLanguageCode(languageCode.trim().lowercase())
        return when {
            normalized.isBlank() -> "en"
            normalized in IndianLanguageCatalog.supportedLanguageCodes -> normalized
            else -> "en"
        }
    }

    /** Map old / non-BCP47 codes to ones Android resources + Locale understand. */
    private fun mapLegacyLanguageCode(code: String): String = when (code) {
        "bodo" -> "brx"
        else -> code
    }
}
