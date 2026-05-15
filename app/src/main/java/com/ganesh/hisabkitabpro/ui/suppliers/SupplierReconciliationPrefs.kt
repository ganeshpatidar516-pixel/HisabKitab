package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Context

object SupplierReconciliationPrefs {
    private const val PREFS = "supplier_reconciliation_prefs"

    fun setVerified(context: Context, supplierId: Long, verified: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("verified_$supplierId", verified)
            .apply()
    }

    fun isVerified(context: Context, supplierId: Long): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("verified_$supplierId", false)
    }

    fun setLastRequestedAt(context: Context, supplierId: Long, timestamp: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong("requestedAt_$supplierId", timestamp)
            .apply()
    }

    fun getLastRequestedAt(context: Context, supplierId: Long): Long {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("requestedAt_$supplierId", 0L)
    }
}
