package com.ganesh.hisabkitabpro.domain.support

import android.content.Context
import android.content.Intent
import com.ganesh.hisabkitabpro.R

object AppShareActions {

    fun shareApp(context: Context) {
        val packageName = context.packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        val message = context.getString(R.string.share_app_message, playStoreUrl)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_app_subject))
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_app_chooser_title)))
    }
}
