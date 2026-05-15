package com.ganesh.hisabkitabpro.domain.invoice

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class InvoiceTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val layoutType: TemplateLayoutType,
    val isPremium: Boolean = false
)

object TemplateRegistry {
    val templates = listOf(
        // ROYAL / FUTURISTIC (10)
        InvoiceTemplate("TEMP_GLASS", "Glassmorphic Pro", "Ultra-modern transparent design.", Icons.Default.AutoAwesome, TemplateLayoutType.MODERN, true),
        InvoiceTemplate("TEMP_ROYAL", "Royal Gold Elite", "Premium gold accents for luxury feel.", Icons.Default.WorkspacePremium, TemplateLayoutType.PREMIUM, true),
        InvoiceTemplate("TEMP_CYBER", "Cyber-Dark Mode", "Neon tech design for modern businesses.", Icons.Default.Terminal, TemplateLayoutType.MODERN, true),
        InvoiceTemplate("TEMP_NEO", "Neo-Brutalism", "Bold shapes and high contrast.", Icons.Default.GridGoldenratio, TemplateLayoutType.MODERN, true),
        InvoiceTemplate("TEMP_AURORA", "Aurora Gradient", "Soft colorful gradients.", Icons.Default.WbTwilight, TemplateLayoutType.MODERN, true),
        InvoiceTemplate("TEMP_TECH", "Tech Blue Grid", "Engineering style blueprint design.", Icons.Default.DeveloperBoard, TemplateLayoutType.DETAILED, true),
        InvoiceTemplate("TEMP_CARBON", "Carbon Fiber Pro", "Sleek industrial texture look.", Icons.Default.SettingsInputComponent, TemplateLayoutType.STANDARD, true),
        InvoiceTemplate("TEMP_VELVET", "Velvet Premium", "Deep maroon elegant design.", Icons.Default.Grade, TemplateLayoutType.PREMIUM, true),
        InvoiceTemplate("TEMP_HOLO", "Hologram Style", "Futuristic light-refraction design.", Icons.Default.AllInclusive, TemplateLayoutType.MODERN, true),
        InvoiceTemplate("TEMP_MIN_PRO", "Minimalist Pro", "Cleanest professional layout.", Icons.Default.CropFree, TemplateLayoutType.COMPACT, true),

        // CLASSIC (5)
        InvoiceTemplate("TEMP_SIMPLE", "Simple Clean", "Standard black & white layout.", Icons.Default.Description, TemplateLayoutType.STANDARD),
        InvoiceTemplate("TEMP_GST_STD", "GST Standard", "Compliant with tax regulations.", Icons.Default.AccountBalance, TemplateLayoutType.DETAILED),
        InvoiceTemplate("TEMP_RETAIL", "Retail Thermal", "Optimized for shop receipts.", Icons.Default.Storefront, TemplateLayoutType.COMPACT),
        InvoiceTemplate("TEMP_WHOLESALE", "Wholesale Bulk", "Best for large quantity bills.", Icons.Default.ShoppingCart, TemplateLayoutType.DETAILED),
        InvoiceTemplate("TEMP_BUSINESS", "Business Elite", "Classic corporate template.", Icons.Default.Business, TemplateLayoutType.STANDARD)
    )

    fun getTemplateById(id: String): InvoiceTemplate {
        return templates.find { it.id == id } ?: templates.first { it.id == "TEMP_SIMPLE" }
    }

    fun getAllTemplates(): List<InvoiceTemplate> {
        return templates
    }

    /**
     * P2 — templates shown in Settings → Invoice Templates.
     * PDF engine today supports **Standard** (black header) and **Modern** (blue header) only.
     */
    fun getBillPdfPickerTemplates(): List<InvoiceTemplate> = listOf(
        getTemplateById("TEMP_SIMPLE"),
        getTemplateById("TEMP_GST_STD"),
        getTemplateById("TEMP_RETAIL"),
        getTemplateById("TEMP_BUSINESS"),
        getTemplateById("TEMP_GLASS"),
    )

    /**
     * Maps decorative picker ids to the PDF styles we actually render today.
     */
    fun normalizePdfTemplateId(templateId: String?): String {
        val id = templateId?.trim().orEmpty()
        val resolved = if (id.startsWith("TEMP_")) {
            templates.find { it.id == id } ?: templates.first { it.id == "TEMP_SIMPLE" }
        } else {
            templates.first { it.id == "TEMP_SIMPLE" }
        }
        return when (resolved.layoutType) {
            TemplateLayoutType.MODERN,
            TemplateLayoutType.PREMIUM -> "TEMP_GLASS"
            else -> "TEMP_SIMPLE"
        }
    }
}
