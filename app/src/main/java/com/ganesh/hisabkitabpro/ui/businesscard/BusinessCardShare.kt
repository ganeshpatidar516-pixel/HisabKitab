package com.ganesh.hisabkitabpro.ui.businesscard

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Centralised share intents for the engine — keeps the UI screen presentation-only. */
object BusinessCardShare {

    fun sharePdf(context: Context, uri: Uri, chooserTitle: String = "Share Business Card Sheet") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareImage(context: Context, uri: Uri, chooserTitle: String = "Share Business Card") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
