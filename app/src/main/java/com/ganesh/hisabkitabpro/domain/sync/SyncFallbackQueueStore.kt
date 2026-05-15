package com.ganesh.hisabkitabpro.domain.sync

import android.content.Context
import android.util.Log
/**
 * P2 — persists the in-memory sync parking lot across process death.
 * Does not touch ledger tables; only mirrors [SyncEngine]'s RAM fallback queue.
 */
object SyncFallbackQueueStore {

    private const val TAG = "SyncFallbackQueue"
    private const val PREFS = "sync_fallback_queue"
    private const val KEY_JSON = "items_json"

    fun load(context: Context): List<SyncItem> {
        val json = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
            ?: return emptyList()
        return runCatching { SyncFallbackQueueCodec.decode(json) }
            .getOrElse {
                Log.w(TAG, "Failed to parse persisted fallback queue", it)
                emptyList()
            }
    }

    fun save(context: Context, items: List<SyncItem>) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (items.isEmpty()) {
            prefs.edit().remove(KEY_JSON).commit()
            return
        }
        prefs.edit().putString(KEY_JSON, SyncFallbackQueueCodec.encode(items)).commit()
    }
}
