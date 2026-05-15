package com.ganesh.hisabkitabpro.domain.ledger

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.data.repository.local.CustomerDao
import com.ganesh.hisabkitabpro.data.repository.local.TransactionDao
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * P2 — read-only drift detector: compares [com.ganesh.hisabkitabpro.domain.model.Customer.balanceCache]
 * with SQL [TransactionDao.calculateBalance]. Does not auto-repair (sacred ledger safety).
 */
object BalanceCacheReconciler {

    private const val TAG = "BalanceCacheReconciler"
    private const val MAX_CUSTOMERS = 500

    data class DriftReport(
        val customersChecked: Int,
        val driftCount: Int,
        val sampleCustomerIds: List<Long>,
    )

    suspend fun logDriftIfAny(
        appContext: Context,
        customerDao: CustomerDao,
        transactionDao: TransactionDao,
        maxCustomers: Int = MAX_CUSTOMERS,
    ): DriftReport {
        val ids = customerDao.getActiveCustomerIds().take(maxCustomers.coerceAtLeast(1))
        val drifts = mutableListOf<Long>()
        for (id in ids) {
            val customer = customerDao.getCustomerById(id) ?: continue
            val sqlBalance = transactionDao.calculateBalance(id)
            if (customer.balanceCache != sqlBalance) {
                drifts.add(id)
            }
        }
        val report = DriftReport(
            customersChecked = ids.size,
            driftCount = drifts.size,
            sampleCustomerIds = drifts.take(5),
        )
        if (report.driftCount > 0) {
            Log.w(
                TAG,
                "balance_cache_drift checked=${report.customersChecked} drift=${report.driftCount} " +
                    "sample=${report.sampleCustomerIds}",
            )
            recordCrashlyticsBreadcrumb(appContext, report)
        } else {
            Log.i(TAG, "balance_cache_ok checked=${report.customersChecked}")
        }
        return report
    }

    private fun recordCrashlyticsBreadcrumb(appContext: Context, report: DriftReport) {
        runCatching {
            if (FirebaseApp.getApps(appContext).isEmpty()) return
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!crashlytics.isCrashlyticsCollectionEnabled) return
            crashlytics.log(
                "balance_cache_drift count=${report.driftCount} checked=${report.customersChecked}",
            )
            crashlytics.setCustomKey("balance_cache_drift_count", report.driftCount)
        }
    }
}
