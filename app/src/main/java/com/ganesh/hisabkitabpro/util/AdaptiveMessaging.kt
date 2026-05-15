package com.ganesh.hisabkitabpro.util

import android.content.Context
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.core.locale.AppLocaleManager
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.profile.ProfileMapFooter
import java.text.NumberFormat
import java.util.Locale

object AdaptiveMessaging {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    /**
     * @param profile optional business profile — when set, appends a professional merchant footer
     * (phone, UPI, etc.) from [ProfileMapFooter] so bulk reminders stay on-brand.
     */
    fun getPaymentReminder(
        context: Context,
        name: String,
        amountPaise: Long,
        daysOverdue: Int = 0,
        profile: BusinessProfile? = null,
    ): String {
        val lc = AppLocaleManager.wrapContext(context)
        val amount = currencyFormatter.format(amountPaise / 100.0)
        val resId = when {
            daysOverdue <= 0 -> R.string.reminder_adaptive_0
            daysOverdue in 1..7 -> R.string.reminder_adaptive_1
            daysOverdue in 8..14 -> R.string.reminder_adaptive_2
            else -> R.string.reminder_adaptive_3
        }
        val body = lc.getString(resId, name, amount)
        val footer = ProfileMapFooter.mapFooter(profile) ?: return body
        return "$body\n\n$footer"
    }

    /**
     * Quick WhatsApp balance ping ([R.string.reminder_wa_simple]) plus optional merchant footer.
     */
    fun getSimplePaymentReminder(context: Context, amount: Double, profile: BusinessProfile? = null): String {
        val lc = AppLocaleManager.wrapContext(context)
        val amountStr = currencyFormatter.format(amount)
        val body = lc.getString(R.string.reminder_wa_simple, amountStr)
        val footer = ProfileMapFooter.mapFooter(profile) ?: return body
        return "$body\n\n$footer"
    }
}
