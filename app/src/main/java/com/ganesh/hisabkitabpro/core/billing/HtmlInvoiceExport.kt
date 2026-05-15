package com.ganesh.hisabkitabpro.core.billing

import android.content.Context
import com.ganesh.hisabkitabpro.domain.model.UniversalInvoice

/**
 * Builds invoice HTML using the template chosen in [TemplatePickerScreen]
 * ([PREFS_KEY_HTML_INVOICE_TEMPLATE_ID] in `hisabkitab_prefs`). Falls back to [RoyalGoldTemplate].
 */
object HtmlInvoiceExport {

    fun buildHtml(context: Context, invoice: UniversalInvoice): String {
        val prefs = context.getSharedPreferences("hisabkitab_prefs", Context.MODE_PRIVATE)
        val id = prefs.getString(PREFS_KEY_HTML_INVOICE_TEMPLATE_ID, null)
        val template = id?.let { HtmlTemplateCatalog.byId(it) } ?: RoyalGoldTemplate()
        return template.getHtml(invoice)
    }
}
