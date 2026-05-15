package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Context

object SupplierProfilePrefs {
    private const val PREFS = "supplier_profile_prefs"

    fun setCity(context: Context, supplierId: Long, city: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("city_$supplierId", city.trim())
            .apply()
    }

    fun getCity(context: Context, supplierId: Long): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("city_$supplierId", "")
            .orEmpty()
    }
}
