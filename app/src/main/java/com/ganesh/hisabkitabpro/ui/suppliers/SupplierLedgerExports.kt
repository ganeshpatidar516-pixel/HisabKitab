package com.ganesh.hisabkitabpro.ui.suppliers

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.ganesh.hisabkitabpro.R
import com.ganesh.hisabkitabpro.addon.audit.AuditLogEntry
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import com.ganesh.hisabkitabpro.domain.model.BusinessProfile
import com.ganesh.hisabkitabpro.domain.model.Party
import com.ganesh.hisabkitabpro.domain.profile.StatementPdfBranding
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun exportSupplierLedgerCsv(context: Context, supplier: Party, logs: List<AuditLogEntry>) {
    if (logs.isEmpty()) return
    val csv = buildString {
        append(context.getString(R.string.supplier_csv_header_line))
        append('\n')
        logs.forEach { row ->
            val amount = parseAmountPaise(row.detail) ?: 0L
            val parsed = SupplierLedgerDetailParser.parse(row.detail)
            val tag = (if (parsed.showTag) parsed.tagRaw else "").replace(",", ";")
            val note = parsed.noteDisplay.replace(",", ";")
            val time = SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault()).format(Date(row.createdAt))
            append("${row.action},${amount / 100.0},$tag,$note,$time\n")
        }
    }
    val file = File(
        AppStoragePaths.exportsCacheDir(context),
        "supplier_${supplier.id}_ledger.csv",
    )
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.supplier_export_csv_subject, supplier.name))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.supplier_export_csv_chooser)))
}

internal fun exportSupplierLedgerPdf(
    context: Context,
    supplier: Party,
    businessProfile: BusinessProfile?,
    logs: List<AuditLogEntry>,
    balancePaise: Long,
    formatter: NumberFormat,
) {
    if (logs.isEmpty()) return
    val pdf = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
    }
    val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#B8860B")
        textSize = 26f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val headerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        typeface = android.graphics.Typeface.DEFAULT
    }
    val tableHeadPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var y = 56f
    val margin = 32f
    val appCtx = context.applicationContext

    val na = context.getString(R.string.supplier_pdf_na)
    val defaultBiz = context.getString(R.string.supplier_pdf_default_business)
    val businessName = businessProfile?.businessName?.trim()?.ifBlank { null } ?: defaultBiz
    val businessPhone = businessProfile?.phone?.trim()?.ifBlank { null } ?: na
    val businessAddress = businessProfile?.address?.trim()?.ifBlank { null }
    val businessGstin = businessProfile?.gstNumber?.trim()?.ifBlank { null } ?: na

    val titleLeft = StatementPdfBranding.titleStartXAfterBranding(appCtx, canvas, businessProfile, margin, y)
    canvas.drawText(businessName, titleLeft, y, titlePaint)
    y += 18f
    canvas.drawText(context.getString(R.string.supplier_pdf_powered_by), margin, y, headerPaint)
    y += 16f
    businessProfile?.ownerName?.trim()?.takeIf { it.isNotEmpty() }?.let { owner ->
        canvas.drawText(owner, margin, y, headerPaint)
        y += 14f
    }
    businessAddress?.let {
        canvas.drawText(it.take(72), margin, y, headerPaint)
        y += 14f
    }
    y += 8f
    canvas.drawText(context.getString(R.string.supplier_pdf_business_line, businessName), margin, y, headerPaint)
    y += 14f
    canvas.drawText(context.getString(R.string.supplier_pdf_phone_gst_line, businessPhone, businessGstin), margin, y, headerPaint)

    y += 24f
    canvas.drawText(context.getString(R.string.supplier_pdf_heading), margin, y, tableHeadPaint)
    y += 18f
    canvas.drawText(context.getString(R.string.supplier_pdf_supplier_line, supplier.name), margin, y, paint)
    canvas.drawText(context.getString(R.string.supplier_pdf_date_line, dateFmt.format(Date())), 350f, y, paint)
    y += 14f
    canvas.drawText(
        context.getString(R.string.supplier_pdf_supplier_phone_line, supplier.phone.ifBlank { na }),
        margin,
        y,
        paint,
    )
    y += 20f
    canvas.drawText(
        context.getString(R.string.supplier_pdf_closing_payable, formatter.format(balancePaise / 100.0)),
        margin,
        y,
        tableHeadPaint,
    )

    y += 24f
    val bg = android.graphics.Paint().apply { color = android.graphics.Color.LTGRAY }
    canvas.drawRect(28f, y - 14f, 568f, y + 8f, bg)
    canvas.drawText(context.getString(R.string.supplier_pdf_col_date), 34f, y, tableHeadPaint)
    canvas.drawText(context.getString(R.string.supplier_pdf_col_details), 132f, y, tableHeadPaint)
    canvas.drawText(context.getString(R.string.supplier_pdf_col_type), 392f, y, tableHeadPaint)
    canvas.drawText(context.getString(R.string.supplier_pdf_col_amount), 468f, y, tableHeadPaint)
    y += 22f

    logs.take(24).forEach {
        if (y > 790f) return@forEach
        val amount = parseAmountPaise(it.detail) ?: 0L
        val parsed = SupplierLedgerDetailParser.parse(it.detail)
        val isPayment = it.action.contains("PAYMENT", ignoreCase = true)
        paint.color = android.graphics.Color.BLACK
        canvas.drawText(dateFmt.format(Date(it.createdAt)), 34f, y, paint)
        val details = parsed.noteDisplay.takeIf { d -> d.isNotBlank() } ?: it.action
        canvas.drawText(details.take(34), 132f, y, paint)
        paint.color = if (isPayment) android.graphics.Color.parseColor("#388E3C") else android.graphics.Color.parseColor("#D32F2F")
        canvas.drawText(
            if (isPayment) context.getString(R.string.supplier_pdf_type_paid)
            else context.getString(R.string.supplier_pdf_type_purchase),
            392f,
            y,
            paint,
        )
        canvas.drawText(formatter.format(amount / 100.0), 468f, y, paint)
        y += 20f
    }

    y += 6f
    paint.color = android.graphics.Color.BLACK
    canvas.drawLine(28f, y, 568f, y, paint)
    y += 22f
    canvas.drawText(
        context.getString(R.string.supplier_pdf_final_payable, formatter.format(balancePaise / 100.0)),
        margin,
        y,
        tableHeadPaint,
    )

    pdf.finishPage(page)
    val safeName = supplier.name
        .trim()
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^A-Za-z0-9_-]"), "")
        .ifBlank { context.getString(R.string.supplier_pdf_file_fallback_name) }
    val file = File(
        AppStoragePaths.exportsCacheDir(context),
        "${safeName}_Statement_${System.currentTimeMillis()}.pdf",
    )
    file.outputStream().use { pdf.writeTo(it) }
    pdf.close()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.supplier_export_pdf_subject, supplier.name))
        putExtra(Intent.EXTRA_TITLE, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
        Intent.createChooser(shareIntent, context.getString(R.string.supplier_export_pdf_chooser, file.nameWithoutExtension)),
    )
}
