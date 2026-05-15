package com.ganesh.hisabkitabpro.domain.inventory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryCommandParserTest {

    @Test
    fun parsesInventorySummary() {
        val command = InventoryCommandParser.parse("inventory summary dikhao")
        assertTrue(command is InventoryCommandParser.Command.Summary)
    }

    @Test
    fun parsesLowStockQuery() {
        val command = InventoryCommandParser.parse("low stock products batao")
        assertTrue(command is InventoryCommandParser.Command.LowStock)
    }

    @Test
    fun parsesAddProductWithStockPriceAndBarcode() {
        val command = InventoryCommandParser.parse(
            "add product basmati rice stock 12 price 85 barcode 8901234567890"
        )
        require(command is InventoryCommandParser.Command.AddProduct)
        assertEquals("basmati rice", command.name)
        assertEquals(12.0, command.quantity, 0.0001)
        assertEquals(85.0, command.sellingPrice, 0.0001)
        assertEquals("8901234567890", command.barcode)
    }

    @Test
    fun parsesPositiveStockAdjustment() {
        val command = InventoryCommandParser.parse("add 5 stock for sugar")
        require(command is InventoryCommandParser.Command.AdjustStock)
        assertEquals("sugar", command.productName)
        assertEquals(5.0, command.delta, 0.0001)
    }

    @Test
    fun parsesNegativeStockAdjustment() {
        val command = InventoryCommandParser.parse("reduce 2 stock from sugar")
        require(command is InventoryCommandParser.Command.AdjustStock)
        assertEquals("sugar", command.productName)
        assertEquals(-2.0, command.delta, 0.0001)
    }

    @Test
    fun parsesFindProduct() {
        val command = InventoryCommandParser.parse("find product atta")
        require(command is InventoryCommandParser.Command.FindProduct)
        assertEquals("atta", command.query)
    }

    @Test
    fun ignoresLedgerOnlyText() {
        assertNull(InventoryCommandParser.parse("ramesh ko 500 add karo"))
    }
}
