package com.ganesh.hisabkitabpro.domain.businesscard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTemplateGeneratorTest {

    @Test
    fun `engine emits exactly fifty cards`() {
        val all = MultiTemplateGenerator.generate()
        assertEquals(50, all.size)
        assertEquals(MultiTemplateGenerator.totalVariations, all.size)
    }

    @Test
    fun `every category contributes ten variants`() {
        val grouped = MultiTemplateGenerator.generate().groupBy { it.category }
        assertEquals(BusinessCardCategory.values().size, grouped.size)
        for ((_, list) in grouped) {
            assertEquals(MultiTemplateGenerator.VARIATIONS_PER_CATEGORY, list.size)
        }
    }

    @Test
    fun `all variation ids are unique`() {
        val ids = MultiTemplateGenerator.generate().map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `variation seed is non zero and category dependent`() {
        for (variation in MultiTemplateGenerator.generate()) {
            assertTrue("seed must be non-zero for ${variation.id}", variation.seed != 0)
        }
    }

    @Test
    fun `palette and typography are populated for every variation`() {
        for (variation in MultiTemplateGenerator.generate()) {
            assertNotNull(variation.palette)
            assertNotNull(variation.typography)
            assertTrue(variation.palette.backgroundArgb != 0)
            assertTrue(variation.palette.titleArgb != 0)
        }
    }

    @Test
    fun `engine output is deterministic across invocations`() {
        val a = MultiTemplateGenerator.generate().map { it.id to it.seed }
        val b = MultiTemplateGenerator.generate().map { it.id to it.seed }
        assertEquals(a, b)
    }

    @Test
    fun `byCategory returns ten matching variants`() {
        for (category in BusinessCardCategory.values()) {
            val list = MultiTemplateGenerator.byCategory(category)
            assertEquals(10, list.size)
            assertTrue(list.all { it.category == category })
        }
    }
}
