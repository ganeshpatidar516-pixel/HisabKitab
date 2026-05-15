package com.ganesh.hisabkitabpro.data.migration

import android.content.Context
import android.util.Log
import com.ganesh.hisabkitabpro.data.local.AppDatabase
import com.ganesh.hisabkitabpro.ui.suppliers.SupplierProfilePrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies supplier city from [SupplierProfilePrefs] into [com.ganesh.hisabkitabpro.domain.model.Party.city]
 * after DB v44 so paging search can match locality without scanning prefs.
 */
object SupplierPartyCityBackfill {
    private const val TAG = "SupplierPartyCityBackfill"

    suspend fun runIfNeeded(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        val dao = db.partyDao()
        val pending = runCatching { dao.getActiveSuppliersWithEmptyCityColumn() }
            .onFailure { Log.w(TAG, "Empty-city query failed: ${it::class.java.simpleName}") }
            .getOrNull() ?: return@withContext
        if (pending.isEmpty()) return@withContext
        var updated = 0
        for (p in pending) {
            val fromPrefs = SupplierProfilePrefs.getCity(context, p.id).trim()
            if (fromPrefs.isEmpty()) continue
            runCatching {
                dao.updateParty(p.copy(city = fromPrefs, updatedAt = System.currentTimeMillis()))
                updated++
            }.onFailure { Log.w(TAG, "Backfill id=${p.id} failed: ${it::class.java.simpleName}") }
        }
        if (updated > 0) Log.i(TAG, "Synced city from prefs for $updated supplier parties")
    }
}
