package com.ganesh.hisabkitabpro.domain.inventory

/**
 * Pure parser for inventory-related Super AI commands.
 *
 * This deliberately stays outside the ledger parser so inventory phrases like
 * "add 10 rice stock" cannot be misrouted as customer ledger credits.
 */
object InventoryCommandParser {

    sealed interface Command {
        data object Summary : Command
        data object LowStock : Command
        data class FindProduct(val query: String) : Command
        data class AddProduct(
            val name: String,
            val quantity: Double,
            val sellingPrice: Double,
            val barcode: String? = null
        ) : Command
        data class AdjustStock(
            val productName: String,
            val delta: Double
        ) : Command
    }

    fun parse(normalizedInput: String): Command? {
        val input = normalizedInput.trim().lowercase()
        if (input.isBlank()) return null

        val inventoryHint = listOf(
            "inventory",
            "stock",
            "product",
            "item",
            "barcode",
            "sku",
            "maal",
            "samaan"
        ).any { input.contains(it) }
        if (!inventoryHint) return null

        if (input.contains("low stock") ||
            input.contains("kam stock") ||
            input.contains("stock kam") ||
            input.contains("khatam") ||
            input.contains("reorder")
        ) {
            return Command.LowStock
        }

        if (input.contains("summary") ||
            input.contains("report") ||
            input.contains("total") ||
            input.contains("kitne product") ||
            input.contains("inventory value")
        ) {
            return Command.Summary
        }

        parseAddProduct(input)?.let { return it }
        parseAdjustStock(input)?.let { return it }
        parseFindProduct(input)?.let { return it }

        return null
    }

    private fun parseAddProduct(input: String): Command.AddProduct? {
        val addPattern = Regex(
            pattern = "(?:add|create|new|naya)\\s+(?:product|item)?\\s*(.+?)\\s+" +
                "(?:stock|qty|quantity)\\s+([0-9]+(?:\\.[0-9]+)?)\\s+" +
                "(?:price|rate|selling|mrp)\\s+([0-9]+(?:\\.[0-9]+)?)(?:\\s+barcode\\s+([a-z0-9\\-_.]+))?"
        )
        val m = addPattern.find(input) ?: return null
        val name = cleanProductName(m.groupValues[1])
        val qty = m.groupValues[2].toDoubleOrNull() ?: return null
        val price = m.groupValues[3].toDoubleOrNull() ?: return null
        val barcode = m.groupValues.getOrNull(4)?.takeIf { it.isNotBlank() }
        if (name.isBlank() || qty < 0.0 || price < 0.0) return null
        return Command.AddProduct(name, qty, price, barcode)
    }

    private fun parseAdjustStock(input: String): Command.AdjustStock? {
        val plusPattern = Regex(
            pattern = "(?:add|increase|plus|jama|receive|received)\\s+" +
                "([0-9]+(?:\\.[0-9]+)?)\\s+(?:stock|qty|quantity)\\s+(?:for|in|to)?\\s*(.+)"
        )
        plusPattern.find(input)?.let { m ->
            val delta = m.groupValues[1].toDoubleOrNull() ?: return null
            val name = cleanProductName(m.groupValues[2])
            if (name.isNotBlank()) return Command.AdjustStock(name, delta)
        }

        val minusPattern = Regex(
            pattern = "(?:reduce|minus|sell|sold|decrease|ghatao)\\s+" +
                "([0-9]+(?:\\.[0-9]+)?)\\s+(?:stock|qty|quantity)?\\s*(?:of|from|for)?\\s*(.+)"
        )
        minusPattern.find(input)?.let { m ->
            val delta = m.groupValues[1].toDoubleOrNull() ?: return null
            val name = cleanProductName(m.groupValues[2])
            if (name.isNotBlank()) return Command.AdjustStock(name, -delta)
        }

        val productFirst = Regex(
            pattern = "(.+?)\\s+(?:stock|qty|quantity)\\s+" +
                "(?:add|increase|plus|jama|reduce|minus|decrease|ghatao)\\s+" +
                "([0-9]+(?:\\.[0-9]+)?)"
        )
        productFirst.find(input)?.let { m ->
            val verbIsMinus = input.contains("reduce") ||
                input.contains("minus") ||
                input.contains("decrease") ||
                input.contains("ghatao")
            val qty = m.groupValues[2].toDoubleOrNull() ?: return null
            val name = cleanProductName(m.groupValues[1])
            if (name.isNotBlank()) return Command.AdjustStock(name, if (verbIsMinus) -qty else qty)
        }

        return null
    }

    private fun parseFindProduct(input: String): Command.FindProduct? {
        val patterns = listOf(
            Regex("(?:find|search|show|check)\\s+(?:product|item|stock)\\s+(.+)"),
            Regex("(?:product|item|stock)\\s+(.+)\\s+(?:kitna|details|detail|hai)$"),
            Regex("(.+)\\s+(?:stock|inventory)\\s+(?:kitna|details|detail|hai)$")
        )
        patterns.forEach { pattern ->
            pattern.find(input)?.let { m ->
                val query = cleanProductName(m.groupValues[1])
                if (query.isNotBlank()) return Command.FindProduct(query)
            }
        }
        return null
    }

    private fun cleanProductName(raw: String): String {
        return raw
            .replace(Regex("\\b(product|item|stock|inventory|ka|ki|ke|ko|for|of|in|to)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
