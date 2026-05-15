package com.ganesh.hisabkitabpro.commandos.dialect

data class DialectPack(
    val packId: String,
    val locale: String,
    val mappings: Map<String, String>
)

class DialectRegistry {
    private val packs = listOf(
        DialectPack(
            packId = "dialect_pack_rajasthan_kota_v1",
            locale = "hinglish-hi",
            mappings = mapOf(
                "jama kar" to "add",
                "jama kr" to "add",
                "credit kar" to "add",
                "hisaab nipta" to "clear bill",
                "hisab nipta" to "clear bill",
                "bill nipta" to "clear bill",
                "yaad dila" to "send reminder",
                "yaad dilao" to "send reminder",
                "settings kholo" to "open settings",
                "grahak kholo" to "open customers"
            )
        )
    )

    fun apply(input: String, locale: String): String {
        val activePack = packs.firstOrNull { it.locale == locale } ?: return input
        var transformed = input
        activePack.mappings.forEach { (spoken, canonical) ->
            transformed = transformed.replace(spoken, canonical)
        }
        return transformed
    }
}
