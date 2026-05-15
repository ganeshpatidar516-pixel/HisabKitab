package com.ganesh.hisabkitabpro.data.prefs

import android.content.Context

/** Per-supplier credit term days for reminder scheduling. */
object SupplierCreditTermsPrefs {
    private const val PREFS = "supplier_credit_terms_prefs"

    fun getTermDays(context: Context, supplierId: Long): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("supplier_term_days_$supplierId", 30)
            .coerceAtLeast(1)
    }

    fun setTermDays(context: Context, supplierId: Long, days: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt("supplier_term_days_$supplierId", days.coerceAtLeast(1))
            .apply()
    }
}
