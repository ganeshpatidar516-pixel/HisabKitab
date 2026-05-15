package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.sync.ConflictPolicy
import com.ganesh.hisabkitabpro.commandos.sync.ConflictResolution
import com.ganesh.hisabkitabpro.commandos.sync.ConflictResolver
import com.ganesh.hisabkitabpro.commandos.sync.InMemoryOfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.OfflineCommandEntry
import com.ganesh.hisabkitabpro.commandos.sync.OfflineReplayEngine
import com.ganesh.hisabkitabpro.commandos.sync.SyncConflict
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayAndConflictTest {
    @Test
    fun replayEngine_removesOnlySuccessfullyProcessedEntries() = runBlocking {
        val journal = InMemoryOfflineCommandJournal()
        val ok = OfflineCommandEntry("1", "a", "hi", "k1", 1L)
        val fail = OfflineCommandEntry("2", "b", "hi", "k2", 2L)
        journal.enqueue(ok)
        journal.enqueue(fail)
        val engine = OfflineReplayEngine(journal)

        val processed = engine.replayBatch { it.id == "1" }

        assertEquals(1, processed)
        assertEquals(1, journal.size())
        assertEquals("2", journal.peek(1).first().id)
    }

    @Test
    fun conflictResolver_appliesConfiguredPolicy() {
        val c = SyncConflict("k1", "local", "server")
        val local = ConflictResolver(ConflictPolicy.LOCAL_WINS).resolve(c)
        val server = ConflictResolver(ConflictPolicy.SERVER_WINS).resolve(c)
        val manual = ConflictResolver(ConflictPolicy.MANUAL_REVIEW).resolve(c)

        assertTrue(local is ConflictResolution.UseLocal)
        assertTrue(server is ConflictResolution.UseServer)
        assertTrue(manual is ConflictResolution.RequireManualReview)
    }
}
