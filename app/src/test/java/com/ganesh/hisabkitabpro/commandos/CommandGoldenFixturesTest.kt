package com.ganesh.hisabkitabpro.commandos

import com.ganesh.hisabkitabpro.commandos.dialect.DialectRegistry
import com.ganesh.hisabkitabpro.commandos.intent.DeterministicIntentParser
import com.ganesh.hisabkitabpro.commandos.model.IntentName
import com.ganesh.hisabkitabpro.commandos.normalize.InputNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandGoldenFixturesTest {
    private val normalizer = InputNormalizer()
    private val dialectRegistry = DialectRegistry()
    private val parser = DeterministicIntentParser()

    @Test
    fun fixtures_hinglishAndDialectCommands_mapToExpectedIntents() {
        val fixtures = listOf(
            Fixture(
                input = "Ramesh ko paanch sau add karo",
                expectedNormalized = "ramesh ko 500 add karo",
                expectedIntent = IntentName.LEDGER_ADD
            ),
            Fixture(
                input = "Ramesh ka bill clear karo",
                expectedNormalized = "ramesh ka bill clear karo",
                expectedIntent = IntentName.BILL_CLEAR
            ),
            Fixture(
                input = "Ramesh ko yaad dila",
                expectedNormalized = "ramesh ko send reminder",
                expectedIntent = IntentName.REMINDER_SEND
            ),
            Fixture(
                input = "Ganesh ke khate mein ₹500 jodo",
                expectedNormalized = "ganesh ke account me 500 add",
                expectedIntent = IntentName.LEDGER_ADD
            ),
            Fixture(
                input = "Sohan Lal ji ke khate mein ₹5000 jode jaaye",
                expectedNormalized = "sohan lal ji ke account me 5000 add karo",
                expectedIntent = IntentName.LEDGER_ADD
            ),
            Fixture(
                input = "गणेश को ₹500 ऐड करो",
                expectedNormalized = "गणेश ko 500 add karo",
                expectedIntent = IntentName.LEDGER_ADD
            ),
            Fixture(
                input = "setting language hi set karo",
                expectedNormalized = "setting language hi set karo",
                expectedIntent = IntentName.SETTING_UPDATE
            ),
            Fixture(
                input = "sabka balance kitna hai",
                expectedNormalized = "sabka bill kitna hai",
                expectedIntent = IntentName.LEDGER_QUERY
            ),
            Fixture(
                input = "ramesh ka balance kya hai",
                expectedNormalized = "ramesh ka bill kya hai",
                expectedIntent = IntentName.LEDGER_QUERY
            ),
            Fixture(
                input = "kuch random unknown bol",
                expectedNormalized = "kuch random unknown bol",
                expectedIntent = IntentName.UNKNOWN
            )
        )

        fixtures.forEach { fixture ->
            val normalized = normalizer.normalize(fixture.input)
            val dialectAdjusted = dialectRegistry.apply(normalized, "hinglish-hi")
            val parsed = parser.parse(
                rawInput = fixture.input,
                normalizedInput = dialectAdjusted,
                locale = "hinglish-hi"
            )

            assertEquals(fixture.expectedNormalized, dialectAdjusted)
            assertEquals(fixture.expectedIntent, parsed.intent.name)
        }
    }

    private data class Fixture(
        val input: String,
        val expectedNormalized: String,
        val expectedIntent: IntentName
    )
}
