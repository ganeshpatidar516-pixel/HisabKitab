package com.ganesh.hisabkitabpro.domain.ledger

import android.content.Context
import java.io.File

/**
 * Single entry for opening ledger/bill PDFs from UI — delegates to [InvoicePdfGenerator].
 */
object LedgerPdfFacade {

    fun resolveBillPdf(context: Context, transactionId: Long): File? =
        InvoicePdfGenerator.resolveInvoicePdfFile(context, transactionId)

    fun openBillPdf(context: Context, transactionId: Long): Boolean {
        val file = resolveBillPdf(context, transactionId) ?: return false
        InvoicePdfGenerator.openPdfFile(context, file)
        return true
    }

    fun openBillPdf(context: Context, file: File) {
        InvoicePdfGenerator.openPdfFile(context, file)
    }
}
