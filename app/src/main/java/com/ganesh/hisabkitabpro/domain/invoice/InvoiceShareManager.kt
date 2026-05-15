package com.ganesh.hisabkitabpro.domain.invoice

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object InvoiceShareManager {

    fun shareInvoice(
        context: Context,
        pdfFile: File
    ) {

        val uri =
            FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                pdfFile
            )

        val intent = Intent(Intent.ACTION_SEND)

        intent.type = "application/pdf"

        intent.putExtra(
            Intent.EXTRA_STREAM,
            uri
        )

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        context.startActivity(
            Intent.createChooser(
                intent,
                "Share Invoice"
            )
        )
    }
}