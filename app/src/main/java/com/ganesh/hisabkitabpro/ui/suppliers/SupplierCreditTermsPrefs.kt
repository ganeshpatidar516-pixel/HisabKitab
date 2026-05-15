package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Context

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
