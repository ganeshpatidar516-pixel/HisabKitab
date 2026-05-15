package com.ganesh.hisabkitabpro.domain.businesscard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GoldenRatioGridTest {

    @Test
    fun `phi constant matches mathematical golden ratio`() {
        assertEquals(1.6180339887498949, GoldenRatioGrid.PHI, 1e-12)
    }

    @Test
    fun `derived height equals width divided by phi`() {
        val metrics = GoldenRatioGrid.compute(1000f)
        val expected = (1000f / GoldenRatioGrid.PHI).toFloat()
        assertEquals(expected, metrics.heightPx, 0.5f)
    }

    @Test
    fun `bands sum back to width`() {
        val metrics = GoldenRatioGrid.compute(1000f)
        assertEquals(1000f, metrics.majorBandWidth + metrics.minorBandWidth, 0.5f)
    }

    @Test
    fun `safe padding is 1 over phi cubed of width`() {
        val metrics = GoldenRatioGrid.compute(1000f)
        val expected = (1000f / (GoldenRatioGrid.PHI * GoldenRatioGrid.PHI * GoldenRatioGrid.PHI)).toFloat()
        assertEquals(expected, metrics.safePadding, 0.5f)
    }

    @Test
    fun `font sizes form a phi geometric sequence`() {
        val metrics = GoldenRatioGrid.compute(1000f)
        assertTrue(abs(metrics.titleSizePx / metrics.subtitleSizePx - GoldenRatioGrid.PHI) < 0.01)
        assertTrue(abs(metrics.subtitleSizePx / metrics.bodySizePx - GoldenRatioGrid.PHI) < 0.01)
        assertTrue(abs(metrics.bodySizePx / metrics.microSizePx - GoldenRatioGrid.PHI) < 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non positive width is rejected`() {
        GoldenRatioGrid.compute(0f)
    }
}
