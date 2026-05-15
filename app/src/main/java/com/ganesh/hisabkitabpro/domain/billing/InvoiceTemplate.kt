package com.ganesh.hisabkitabpro.domain.billing

enum class InvoiceCategory {
    FUTURISTIC,
    CLASSIC
}

enum class InvoiceTemplateType {
    // Futuristic Designs
    GLASSMORPHIC,
    ROYAL_GOLD,
    CYBER_DARK,
    NEO_BRUTALISM,
    AURORA_GRADIENT,
    TECH_BLUE,
    CARBON_FIBER,
    VELVET_PREMIUM,
    HOLOGRAM_STYLE,
    MINIMAL_PRO,
    
    // Classic Designs
    SIMPLE_CLEAN,
    GST_STANDARD,
    RETAIL_THERMAL,
    WHOLESALE_BULK,
    BUSINESS_ELITE
}

data class InvoiceTemplate(
    val id: String,
    val name: String,
    val type: InvoiceTemplateType,
    val category: InvoiceCategory,
    val isPremium: Boolean = false
)
