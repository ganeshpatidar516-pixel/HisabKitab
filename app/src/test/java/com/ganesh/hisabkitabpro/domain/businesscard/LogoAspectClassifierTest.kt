package com.ganesh.hisabkitabpro.domain.businesscard

import org.junit.Assert.assertEquals
import org.junit.Test

class LogoAspectClassifierTest {

    @Test
    fun `returns square fallback for invalid dimensions`() {
        assertEquals(LogoAspectClassifier.LogoShape.SQUARE, LogoAspectClassifier.classify(0, 100))
        assertEquals(LogoAspectClassifier.LogoShape.SQUARE, LogoAspectClassifier.classify(100, 0))
    }

    @Test
    fun `near square logos classify as square`() {
        assertEquals(LogoAspectClassifier.LogoShape.SQUARE, LogoAspectClassifier.classify(512, 512))
        assertEquals(LogoAspectClassifier.LogoShape.SQUARE, LogoAspectClassifier.classify(500, 510))
    }

    @Test
    fun `near square logos classify as circle when caller prefers`() {
        assertEquals(LogoAspectClassifier.LogoShape.CIRCLE, LogoAspectClassifier.classify(512, 512, prefersCircular = true))
    }

    @Test
    fun `wide logos classify as landscape`() {
        assertEquals(LogoAspectClassifier.LogoShape.LANDSCAPE, LogoAspectClassifier.classify(800, 320))
    }

    @Test
    fun `tall logos classify as portrait`() {
        assertEquals(LogoAspectClassifier.LogoShape.PORTRAIT, LogoAspectClassifier.classify(400, 1000))
    }
}
