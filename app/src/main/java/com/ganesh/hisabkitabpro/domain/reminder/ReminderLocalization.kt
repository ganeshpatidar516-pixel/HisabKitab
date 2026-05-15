package com.ganesh.hisabkitabpro.domain.reminder

import android.content.Context
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import java.util.Locale

object ReminderLocalization {
    fun resolveLanguageCode(context: Context): String {
        val saved = AppLocaleManager.normalizeLanguageCode(AppLocaleManager.getSavedLanguageCode(context))
        return if (saved == "system") {
            Locale.getDefault().language.lowercase().ifBlank { "en" }
        } else {
            saved
        }
    }

    private fun lc(context: Context): Context = AppLocaleManager.wrapContext(context)

    fun noDueBalanceText(context: Context): String =
        lc(context).getString(R.string.reminder_toast_no_due)

    fun phoneUnavailableText(context: Context): String =
        lc(context).getString(R.string.reminder_toast_phone_missing)

    fun channelUnavailableText(context: Context): String =
        lc(context).getString(R.string.reminder_toast_channel_failed)

    fun whatsappOpenedText(context: Context): String =
        lc(context).getString(R.string.reminder_toast_opened_whatsapp)

    fun smsOpenedText(context: Context): String =
        lc(context).getString(R.string.reminder_toast_opened_sms)
}
