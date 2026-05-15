package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeterministicIntentParserTest {
    private val parser = DeterministicIntentParser()

    @Test
    fun parse_detectsLedgerAddIntentAndEntities() {
        val parsed = parser.parse(
            rawInput = "Ramesh ko 500 add karo",
            normalizedInput = "ramesh ko 500 add karo",
            locale = "hinglish-hi"
        )

        assertEquals(IntentName.LEDGER_ADD, parsed.intent.name)
        assertEquals("ramesh", parsed.entities.customerName)
        assertNotNull(parsed.entities.amount)
        assertEquals(500L, parsed.entities.amount)
    }

    @Test
    fun parse_unknownInputReturnsUnknownIntent() {
        val parsed = parser.parse(
            rawInput = "kuch bhi random",
            normalizedInput = "kuch bhi random",
            locale = "hinglish-hi"
        )
        assertEquals(IntentName.UNKNOWN, parsed.intent.name)
    }

    @Test
    fun parse_customerAddWithPhone_detectsIntentAndPhone() {
        val parsed = parser.parse(
            rawInput = "Ganesh naam ka naya customer add phone 9876543210",
            normalizedInput = "ganesh naam ka naya customer add phone 9876543210",
            locale = "hinglish-hi"
        )
        assertEquals(IntentName.CUSTOMER_ADD, parsed.intent.name)
        assertEquals("ganesh", parsed.entities.customerName)
        assertEquals("9876543210", parsed.entities.customerPhone)
    }

    @Test
    fun parse_openScreen_detectsRoute() {
        val parsed = parser.parse(
            rawInput = "open settings",
            normalizedInput = "open settings",
            locale = "hinglish-hi"
        )
        assertEquals(IntentName.OPEN_SCREEN, parsed.intent.name)
        assertEquals("settings", parsed.entities.targetRoute)
    }

    @Test
    fun parse_fuzzyLedgerTypos_stillDetectsLedgerAdd() {
        val parsed = parser.parse(
            rawInput = "ganesh ko 500 ad kro",
            normalizedInput = "ganesh ko 500 ad kro",
            locale = "hinglish-hi"
        )
        assertEquals(IntentName.LEDGER_ADD, parsed.intent.name)
        assertEquals("ganesh", parsed.entities.customerName)
        assertEquals(500L, parsed.entities.amount)
    }

    @Test
    fun parse_fuzzyReminderTypos_stillDetectsReminder() {
        val parsed = parser.parse(
            rawInput = "ganesh ko remindr bhejo",
            normalizedInput = "ganesh ko remindr bhejo",
            locale = "hinglish-hi"
        )
        assertEquals(IntentName.REMINDER_SEND, parsed.intent.name)
        assertEquals("ganesh", parsed.entities.customerName)
    }
}
