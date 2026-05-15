package com.ganesh.hisabkitabpro.domain.sync

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** JSON codec for [SyncFallbackQueueStore] — unit-testable without Android Context. */
internal object SyncFallbackQueueCodec {

    private data class Persisted(
        val type: String,
        val payload: String,
        val status: String = SyncStatus.PENDING.name,
    )

    fun encode(items: List<SyncItem>, gson: Gson = Gson()): String {
        val payload = items.map { Persisted(it.type, it.payload, it.status.name) }
        return gson.toJson(payload)
    }

    fun decode(json: String, gson: Gson = Gson()): List<SyncItem> {
        val type = object : TypeToken<List<Persisted>>() {}.type
        val list: List<Persisted> = gson.fromJson(json, type)
        return list.mapNotNull { row ->
            val status = runCatching { SyncStatus.valueOf(row.status) }
                .getOrDefault(SyncStatus.PENDING)
            if (row.type.isBlank() || row.payload.isBlank()) null
            else SyncItem(id = "", type = row.type, payload = row.payload, status = status)
        }
    }
}
