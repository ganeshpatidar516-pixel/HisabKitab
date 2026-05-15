package com.ganesh.hisabkitabpro.commandos.sync

import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

data class OfflineCommandEntry(
    val id: String = UUID.randomUUID().toString(),
    val rawCommand: String,
    val locale: String,
    val idempotencyKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

interface OfflineCommandJournal {
    fun enqueue(entry: OfflineCommandEntry)
    fun peek(limit: Int = 20): List<OfflineCommandEntry>
    fun remove(id: String): Boolean
    fun size(): Int
}

class InMemoryOfflineCommandJournal : OfflineCommandJournal {
    private val queue = ConcurrentLinkedQueue<OfflineCommandEntry>()

    override fun enqueue(entry: OfflineCommandEntry) {
        queue.add(entry)
    }

    override fun peek(limit: Int): List<OfflineCommandEntry> {
        return queue.toList().take(limit)
    }

    override fun remove(id: String): Boolean {
        val current = queue.toList().firstOrNull { it.id == id } ?: return false
        return queue.remove(current)
    }

    override fun size(): Int = queue.size
}
