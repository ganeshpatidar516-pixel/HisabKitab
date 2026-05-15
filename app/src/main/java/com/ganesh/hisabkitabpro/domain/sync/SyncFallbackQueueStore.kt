package com.ganesh.hisabkitabpro.domain.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * P2 — persists the in-memory sync parking lot across process death.
 * Does not touch ledger tables; only mirrors [SyncEngine]'s RAM fallback queue.
 */
object SyncFallbackQueueStore {

    private const val TAG = "SyncFallbackQueue"
    private const val PREFS = "sync_fallback_queue"
    private const val KEY_JSON = "items_json"
    private val gson = Gson()

    private data class Persisted(
        val type: String,
        val payload: String,
        val status: String = SyncStatus.PENDING.name,
    )

    fun load(context: Context): List<SyncItem> {
        val json = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_JSON, null)
            ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Persisted>>() {}.type
            val list: List<Persisted> = gson.fromJson(json, type)
            list.mapNotNull { row ->
                val status = runCatching { SyncStatus.valueOf(row.status) }
                    .getOrDefault(SyncStatus.PENDING)
                if (row.type.isBlank() || row.payload.isBlank()) null
                else SyncItem(id = "", type = row.type, payload = row.payload, status = status)
            }
        }.getOrElse {
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
        val payload = items.map { Persisted(it.type, it.payload, it.status.name) }
        prefs.edit().putString(KEY_JSON, gson.toJson(payload)).commit()
    }
}
