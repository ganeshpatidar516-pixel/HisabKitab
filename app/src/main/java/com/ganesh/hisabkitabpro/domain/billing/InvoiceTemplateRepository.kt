package com.ganesh.hisabkitabpro.domain.billing

object InvoiceTemplateRepository {

    val templates = listOf(
        // FUTURISTIC CATEGORY (10 Designs)
        InvoiceTemplate("TEMP_GLASS", "Glassmorphic Pro", InvoiceTemplateType.GLASSMORPHIC, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_ROYAL", "Royal Gold Elite", InvoiceTemplateType.ROYAL_GOLD, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_CYBER", "Cyber-Dark Mode", InvoiceTemplateType.CYBER_DARK, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_NEO", "Neo-Brutalism", InvoiceTemplateType.NEO_BRUTALISM, InvoiceCategory.FUTURISTIC),
        InvoiceTemplate("TEMP_AURORA", "Aurora Gradient", InvoiceTemplateType.AURORA_GRADIENT, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_TECH", "Tech Blue Grid", InvoiceTemplateType.TECH_BLUE, InvoiceCategory.FUTURISTIC),
        InvoiceTemplate("TEMP_CARBON", "Carbon Fiber Pro", InvoiceTemplateType.CARBON_FIBER, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_VELVET", "Velvet Premium", InvoiceTemplateType.VELVET_PREMIUM, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_HOLO", "Hologram Style", InvoiceTemplateType.HOLOGRAM_STYLE, InvoiceCategory.FUTURISTIC, true),
        InvoiceTemplate("TEMP_MIN_PRO", "Minimalist Pro", InvoiceTemplateType.MINIMAL_PRO, InvoiceCategory.FUTURISTIC),

        // CLASSIC CATEGORY (5 Designs)
        InvoiceTemplate("TEMP_SIMPLE", "Simple Clean", InvoiceTemplateType.SIMPLE_CLEAN, InvoiceCategory.CLASSIC),
        InvoiceTemplate("TEMP_GST_STD", "GST Standard", InvoiceTemplateType.GST_STANDARD, InvoiceCategory.CLASSIC),
        InvoiceTemplate("TEMP_RETAIL", "Retail Thermal", InvoiceTemplateType.RETAIL_THERMAL, InvoiceCategory.CLASSIC),
        InvoiceTemplate("TEMP_WHOLESALE", "Wholesale Bulk", InvoiceTemplateType.WHOLESALE_BULK, InvoiceCategory.CLASSIC),
        InvoiceTemplate("TEMP_BUSINESS", "Business Elite", InvoiceTemplateType.BUSINESS_ELITE, InvoiceCategory.CLASSIC)
    )

    fun getTemplate(id: String): InvoiceTemplate {
        return templates.find { it.id == id } ?: templates.first { it.id == "TEMP_SIMPLE" }
    }
}
