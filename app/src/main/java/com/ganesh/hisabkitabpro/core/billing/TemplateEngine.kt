package com.ganesh.hisabkitabpro.core.billing

import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.domain.model.UniversalInvoice
import java.util.Locale

/**
 * HTML invoice layout contract. Distinct from Compose/PDF [com.ganesh.hisabkitabpro.domain.invoice.InvoiceTemplate].
 */
interface HtmlInvoiceTemplate {
    val id: String
    val name: String
    val previewImage: Int
    fun getHtml(invoice: UniversalInvoice): String
}

internal fun htmlEscape(raw: String): String = buildString {
    for (c in raw) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(c)
        }
    }
}

internal fun htmlItemRows(invoice: UniversalInvoice): String {
    val sym = htmlEscape(invoice.currencySymbol)
    return invoice.items.joinToString("") {
        val itemName = htmlEscape(it.name)
        val line = String.format(Locale.US, "%.2f", it.totalPrice)
        "<tr><td>$itemName</td><td>${it.quantity}</td><td>$sym$line</td></tr>"
    }
}

internal fun htmlGrandTotal(invoice: UniversalInvoice): String {
    val sym = htmlEscape(invoice.currencySymbol)
    val total = String.format(Locale.US, "%.2f", invoice.grandTotal)
    return "$sym$total"
}

private fun invoiceShell(
    invoice: UniversalInvoice,
    bodyStyle: String,
    innerStyle: String,
    titleStyle: String,
    rows: String = htmlItemRows(invoice),
    totalLine: String = htmlGrandTotal(invoice)
): String {
    val business = htmlEscape(invoice.businessName)
    val customer = htmlEscape(invoice.customerName)
    return """
        <html>
        <body style="$bodyStyle">
            <div style="$innerStyle">
                <h1 style="$titleStyle">$business</h1>
                <hr>
                <p>Customer: $customer</p>
                <table style="width:100%;border-collapse:collapse;">
                    <tr><th align="left">Item</th><th>Qty</th><th align="right">Total</th></tr>
                    $rows
                </table>
                <div style="text-align:right;margin-top:24px;">
                    <h2>Total: $totalLine</h2>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}

class RoyalGoldTemplate : HtmlInvoiceTemplate {
    override val id = "royal_gold"
    override val name = "The Royal Gold"
    override val previewImage = R.drawable.preview_royal_gold
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#000000;color:#D4AF37;font-family:serif;padding:40px;",
        innerStyle = "border:2px solid #D4AF37;padding:20px;",
        titleStyle = "text-align:center;letter-spacing:5px;"
    )
}

class ExecutiveTemplate : HtmlInvoiceTemplate {
    override val id = "executive_blue"
    override val name = "Executive Blue"
    override val previewImage = R.drawable.preview_html_executive
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#0D1B2A;color:#ECEFF1;font-family:sans-serif;padding:32px;",
        innerStyle = "border:1px solid #E3B505;padding:24px;",
        titleStyle = "text-align:center;color:#E3B505;"
    )
}

class MinimalistTemplate : HtmlInvoiceTemplate {
    override val id = "minimalist"
    override val name = "Minimalist"
    override val previewImage = R.drawable.preview_html_minimalist
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#FAFAFA;color:#212121;font-family:sans-serif;padding:32px;",
        innerStyle = "padding:16px;",
        titleStyle = "text-align:left;border-bottom:2px solid #212121;padding-bottom:8px;"
    )
}

class CyberDarkTemplate : HtmlInvoiceTemplate {
    override val id = "cyber_dark"
    override val name = "Cyber Dark"
    override val previewImage = R.drawable.preview_html_cyber
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#121212;color:#00E5FF;font-family:monospace;padding:28px;",
        innerStyle = "border:1px solid #00E5FF;padding:20px;",
        titleStyle = "text-align:center;text-shadow:0 0 8px #00E5FF;"
    )
}

class VelvetMaroonTemplate : HtmlInvoiceTemplate {
    override val id = "velvet_maroon"
    override val name = "Velvet Maroon"
    override val previewImage = R.drawable.preview_html_velvet
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#4A0E0E;color:#FFD700;font-family:serif;padding:36px;",
        innerStyle = "border:2px solid #FFD700;padding:22px;",
        titleStyle = "text-align:center;"
    )
}

class TechGridTemplate : HtmlInvoiceTemplate {
    override val id = "tech_grid"
    override val name = "Tech Grid"
    override val previewImage = R.drawable.preview_html_tech
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#E8EAF6;color:#1A237E;font-family:sans-serif;padding:32px;",
        innerStyle = "background-color:#FFFFFF;padding:20px;border:1px solid #C5CAE9;",
        titleStyle = "text-align:left;"
    )
}

class AuroraTemplate : HtmlInvoiceTemplate {
    override val id = "aurora"
    override val name = "Aurora"
    override val previewImage = R.drawable.preview_html_aurora
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background:linear-gradient(135deg,#6A11CB,#2575FC);color:#FFFFFF;font-family:sans-serif;padding:32px;",
        innerStyle = "background-color:rgba(255,255,255,0.12);padding:24px;border-radius:12px;",
        titleStyle = "text-align:center;"
    )
}

class CarbonFiberTemplate : HtmlInvoiceTemplate {
    override val id = "carbon"
    override val name = "Carbon Fiber"
    override val previewImage = R.drawable.preview_html_carbon
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#1C1C1C;color:#EEEEEE;font-family:sans-serif;padding:28px;",
        innerStyle = "border:1px solid #616161;padding:20px;",
        titleStyle = "text-align:center;letter-spacing:2px;"
    )
}

class HoloCyanTemplate : HtmlInvoiceTemplate {
    override val id = "holo_cyan"
    override val name = "Holo Cyan"
    override val previewImage = R.drawable.preview_html_holo
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#004D40;color:#E0F2F1;font-family:sans-serif;padding:30px;",
        innerStyle = "border:1px solid #1DE9B6;padding:22px;border-radius:8px;",
        titleStyle = "text-align:center;color:#1DE9B6;"
    )
}

class NeoBrutalTemplate : HtmlInvoiceTemplate {
    override val id = "neo_brutal"
    override val name = "Neo Brutal"
    override val previewImage = R.drawable.preview_html_neo
    override fun getHtml(invoice: UniversalInvoice) = invoiceShell(
        invoice,
        bodyStyle = "background-color:#FFFF00;color:#000000;font-family:sans-serif;padding:24px;",
        innerStyle = "border:4px solid #000000;padding:16px;",
        titleStyle = "text-align:left;text-transform:uppercase;"
    )
}

object HtmlTemplateCatalog {
    fun all(): List<HtmlInvoiceTemplate> = listOf(
        RoyalGoldTemplate(),
        ExecutiveTemplate(),
        MinimalistTemplate(),
        CyberDarkTemplate(),
        VelvetMaroonTemplate(),
        TechGridTemplate(),
        AuroraTemplate(),
        CarbonFiberTemplate(),
        HoloCyanTemplate(),
        NeoBrutalTemplate()
    )

    fun byId(id: String): HtmlInvoiceTemplate? = all().find { it.id == id }
}

/** SharedPreferences key — same file as [com.ganesh.hisabkitabpro.di.AppModule] `hisabkitab_prefs`. */
const val PREFS_KEY_HTML_INVOICE_TEMPLATE_ID = "html_invoice_template_id"
