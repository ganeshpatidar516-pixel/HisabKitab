package com.ganesh.hisabkitabpro.ui.transactions

import android.content.Context

private data class ItemMemory(
    val name: String,
    val qty: String,
    val unit: String,
    val rate: String,
    val gst: String,
    val hsn: String = "",
    val cess: String = "0",
    val mrp: String = "",
    val rateInc: String = "0"
)

internal object ItemQuickMemory {
    private const val PREFS = "bill_item_quick_memory"
    private const val KEY_LIST = "item_list"
    private const val LIMIT = 40

    fun remember(context: Context, line: BillLineUi) {
        val name = line.name.trim()
        if (name.isBlank()) return
        val safe = ItemMemory(
            name = name,
            qty = line.qty.ifBlank { "1" },
            unit = line.unit.ifBlank { "Nos" },
            rate = line.rate,
            gst = line.gstPercent.ifBlank { "0" },
            hsn = line.hsnCode.trim(),
            cess = line.cessPercent.ifBlank { "0" },
            mrp = line.mrp.trim(),
            rateInc = if (line.rateIncludingTax) "1" else "0"
        )
        val map = loadMap(context).toMutableMap()
        map[name.lowercase()] = safe
        val sorted = map.values.sortedByDescending { it.name == name }.take(LIMIT)
        save(context, sorted)
    }

    fun suggest(context: Context, query: String): List<String> {
        val q = query.trim().lowercase()
        val list = load(context)
        if (q.isBlank()) return list.map { it.name }.take(8)
        return list
            .map { it.name }
            .filter { it.lowercase().contains(q) }
            .take(8)
    }

    fun get(context: Context, name: String): BillLineUi? {
        val key = name.trim().lowercase()
        if (key.isBlank()) return null
        val item = loadMap(context)[key] ?: return null
        return BillLineUi(
            name = item.name,
            qty = item.qty,
            unit = item.unit,
            rate = item.rate,
            gstPercent = item.gst,
            hsnCode = item.hsn,
            cessPercent = item.cess,
            mrp = item.mrp,
            rateIncludingTax = item.rateInc == "1"
        )
    }

    private fun load(context: Context): List<ItemMemory> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LIST, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("||").mapNotNull { token -> decode(token) }
    }

    private fun decode(token: String): ItemMemory? {
        val p = token.split("::")
        return when {
            p.size >= 9 -> ItemMemory(
                name = p[0],
                qty = p[1],
                unit = p[2],
                rate = p[3],
                gst = p[4],
                hsn = p[5],
                cess = p[6],
                mrp = p[7],
                rateInc = p[8]
            )
            p.size == 5 -> ItemMemory(name = p[0], qty = p[1], unit = p[2], rate = p[3], gst = p[4])
            else -> null
        }
    }

    private fun loadMap(context: Context): Map<String, ItemMemory> {
        return load(context).associateBy { it.name.lowercase() }
    }

    private fun save(context: Context, list: List<ItemMemory>) {
        val raw = list.joinToString("||") {
            "${it.name}::${it.qty}::${it.unit}::${it.rate}::${it.gst}::${it.hsn}::${it.cess}::${it.mrp}::${it.rateInc}"
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_LIST, raw).apply()
    }
}
