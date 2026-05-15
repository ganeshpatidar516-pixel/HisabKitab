package com.ganesh.hisabkitabpro.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncFallbackQueueCodecTest {

    @Test
    fun encodeDecode_roundTrip() {
        val items = listOf(
            SyncItem(id = "", type = "TRANSACTION", payload = """{"id":1}""", status = SyncStatus.PENDING),
            SyncItem(id = "", type = "CUSTOMER", payload = """{"id":2}""", status = SyncStatus.FAILED),
        )
        val json = SyncFallbackQueueCodec.encode(items)
        val decoded = SyncFallbackQueueCodec.decode(json)
        assertEquals(2, decoded.size)
        assertEquals("TRANSACTION", decoded[0].type)
        assertEquals(SyncStatus.FAILED, decoded[1].status)
    }

    @Test
    fun decode_skipsBlankRows() {
        val json =
            """[{"type":"","payload":"x","status":"PENDING"},{"type":"TRANSACTION","payload":"ok","status":"PENDING"}]"""
        val decoded = SyncFallbackQueueCodec.decode(json)
        assertEquals(1, decoded.size)
        assertEquals("ok", decoded[0].payload)
    }

    @Test
    fun encode_emptyList_isEmptyArray() {
        val json = SyncFallbackQueueCodec.encode(emptyList())
        assertTrue(json.contains("[]"))
    }
}
