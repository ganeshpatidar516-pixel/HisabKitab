package com.ganesh.hisabkitabpro.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class AssistantInputSplitterTest {

    @Test
    fun split_pipeSeparated_returnsThree() {
        val s = "a ko 500 add karo | b ka bill clear karo | c ko reminder bhejo"
        assertEquals(
            listOf("a ko 500 add karo", "b ka bill clear karo", "c ko reminder bhejo"),
            AssistantInputSplitter.splitForSequentialRun(s)
        )
    }

    @Test
    fun split_chainedKaro_returnsMultiple() {
        val s = "Ramesh ko 500 add karo Ramesh ka bill clear karo Ramesh ko reminder bhejo"
        assertEquals(
            listOf(
                "Ramesh ko 500 add karo",
                "Ramesh ka bill clear karo",
                "Ramesh ko reminder bhejo"
            ),
            AssistantInputSplitter.splitForSequentialRun(s)
        )
    }

    @Test
    fun split_singleCommand_unchanged() {
        val s = "Ganesh ko 500 add karo"
        assertEquals(listOf(s), AssistantInputSplitter.splitForSequentialRun(s))
    }
}
