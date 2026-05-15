package com.ganesh.hisabkitabpro.commandos.sync

class OfflineReplayEngine(
    private val journal: OfflineCommandJournal
) {
    suspend fun replayBatch(
        limit: Int = 20,
        processor: suspend (OfflineCommandEntry) -> Boolean
    ): Int {
        var processed = 0
        val items = journal.peek(limit)
        items.forEach { entry ->
            val ok = processor(entry)
            if (ok) {
                if (journal.remove(entry.id)) {
                    processed += 1
                }
            }
        }
        return processed
    }
}
