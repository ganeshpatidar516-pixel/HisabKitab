package com.ganesh.hisabkitabpro.domain.ledger

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.ganesh.hisabkitabpro.core.firebase.ProductionOpsTelemetry
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.data.repository.local.CustomerDao
import com.ganesh.hisabkitabpro.data.repository.local.TransactionDao
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * P2 — drift detector + optional repair (default OFF via [BalanceCacheRepairToggle]).
 * Repair sets [balanceCache] to SQL [TransactionDao.calculateBalance] — does not mutate transactions.
 */
object BalanceCacheReconciler {

    private const val TAG = "BalanceCacheReconciler"
    private const val MAX_CUSTOMERS = 500

    data class DriftReport(
        val customersChecked: Int,
        val driftCount: Int,
        val sampleCustomerIds: List<Long>,
    )

    data class RepairReport(
        val customersChecked: Int,
        val driftCount: Int,
        val repairedCount: Int,
        val skippedBecauseDisabled: Boolean,
    )

    suspend fun logDriftIfAny(
        appContext: Context,
        customerDao: CustomerDao,
        transactionDao: TransactionDao,
        maxCustomers: Int = MAX_CUSTOMERS,
    ): DriftReport {
        val report = detectDrift(customerDao, transactionDao, maxCustomers)
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

    /**
     * Repairs drift only when auto-repair toggle is ON. Safe: adjusts cache column only.
     */
    suspend fun repairDriftIfEnabled(
        appContext: Context,
        database: AppDatabase,
        toggle: BalanceCacheRepairToggle,
        maxCustomers: Int = MAX_CUSTOMERS,
    ): RepairReport {
        if (!toggle.isAutoRepairEnabled()) {
            return RepairReport(
                customersChecked = 0,
                driftCount = 0,
                repairedCount = 0,
                skippedBecauseDisabled = true,
            )
        }
        val customerDao = database.customerDao()
        val transactionDao = database.transactionDao()
        var repaired = 0
        var checked = 0
        var driftCount = 0
        database.withTransaction {
            val ids = customerDao.getActiveCustomerIds().take(maxCustomers.coerceAtLeast(1))
            checked = ids.size
            for (id in ids) {
                val customer = customerDao.getCustomerById(id) ?: continue
                val sqlBalance = transactionDao.calculateBalance(id)
                if (customer.balanceCache != sqlBalance) {
                    driftCount++
                    customerDao.setBalanceCacheAbsolute(id, sqlBalance)
                    repaired++
                }
            }
        }
        if (repaired > 0) {
            Log.i(TAG, "balance_cache_repaired count=$repaired checked=$checked")
            ProductionOpsTelemetry.recordBalanceCacheRepaired(appContext, repaired)
        }
        return RepairReport(
            customersChecked = checked,
            driftCount = driftCount,
            repairedCount = repaired,
            skippedBecauseDisabled = false,
        )
    }

    /** Manual repair from settings — one shot, does not require auto-repair toggle. */
    suspend fun repairDriftNow(
        appContext: Context,
        database: AppDatabase,
        maxCustomers: Int = MAX_CUSTOMERS,
    ): RepairReport {
        val customerDao = database.customerDao()
        val transactionDao = database.transactionDao()
        var repaired = 0
        var checked = 0
        var driftCount = 0
        database.withTransaction {
            val ids = customerDao.getActiveCustomerIds().take(maxCustomers.coerceAtLeast(1))
            checked = ids.size
            for (id in ids) {
                val customer = customerDao.getCustomerById(id) ?: continue
                val sqlBalance = transactionDao.calculateBalance(id)
                if (customer.balanceCache != sqlBalance) {
                    driftCount++
                    customerDao.setBalanceCacheAbsolute(id, sqlBalance)
                    repaired++
                }
            }
        }
        if (driftCount > 0) {
            recordCrashlyticsBreadcrumb(
                appContext,
                DriftReport(checked, driftCount, emptyList()),
            )
        }
        if (repaired > 0) {
            ProductionOpsTelemetry.recordBalanceCacheRepaired(appContext, repaired)
        }
        Log.i(
            TAG,
            "balance_cache_manual_repair repaired=$repaired drift=$driftCount checked=$checked",
        )
        return RepairReport(
            customersChecked = checked,
            driftCount = driftCount,
            repairedCount = repaired,
            skippedBecauseDisabled = false,
        )
    }

    private suspend fun detectDrift(
        customerDao: CustomerDao,
        transactionDao: TransactionDao,
        maxCustomers: Int,
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
        return DriftReport(
            customersChecked = ids.size,
            driftCount = drifts.size,
            sampleCustomerIds = drifts.take(5),
        )
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
