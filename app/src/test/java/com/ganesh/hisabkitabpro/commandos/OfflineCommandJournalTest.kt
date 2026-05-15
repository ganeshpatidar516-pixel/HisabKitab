package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.sync.IdempotencyKeyFactory
import com.ganesh.hisabkitabpro.commandos.sync.InMemoryOfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.OfflineCommandEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineCommandJournalTest {
    @Test
    fun journal_enqueuePeekRemove_behavesDeterministically() {
        val journal = InMemoryOfflineCommandJournal()
        val entry = OfflineCommandEntry(
            rawCommand = "ramesh ko 500 add karo",
            locale = "hinglish-hi",
            idempotencyKey = IdempotencyKeyFactory.from("ramesh ko 500 add karo", "hinglish-hi", 1000L)
        )

        journal.enqueue(entry)
        assertEquals(1, journal.size())
        assertEquals(entry.id, journal.peek(1).first().id)
        assertTrue(journal.remove(entry.id))
        assertEquals(0, journal.size())
    }

    @Test
    fun idempotencyKey_changesWithTimestamp() {
        val k1 = IdempotencyKeyFactory.from("a", "hi", 1L)
        val k2 = IdempotencyKeyFactory.from("a", "hi", 2L)
        assertNotEquals(k1, k2)
    }
}
