package com.ganesh.hisabkitabpro.domain.inventory

/**
 * Normalizes raw Barcode/QR payloads before product lookup.
 *
 * Many retail barcodes are plain EAN/UPC digits, while QR labels may embed a
 * key-value payload. We keep extraction local and deterministic.
 */
object InventoryScanNormalizer {
    fun normalize(rawValue: String): String {
        val raw = rawValue.trim()
        if (raw.isBlank()) return ""

        // Common QR formats: "barcode=890...", "sku=ABC-1", JSON-ish labels.
        val keyed = Regex(
            pattern = "(?:barcode|bar_code|sku|product_code|code)[=:\"\\s]+([A-Za-z0-9_.-]{3,})",
            option = RegexOption.IGNORE_CASE
        ).find(raw)?.groupValues?.getOrNull(1)
        if (!keyed.isNullOrBlank()) return keyed.trim()

        // For plain EAN/UPC/Code128, keep only the readable payload.
        return raw
            .removePrefix("http://")
            .removePrefix("https://")
            .trim()
    }
}
