package com.ganesh.hisabkitabpro.domain.ai.marketing

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object WhatsAppMarketingManager {

    /**
     * विज्ञापन पोस्टर को ग्राहक के WhatsApp पर शेयर करता है।
     */
    fun sharePosterToWhatsApp(context: Context, customerPhone: String, posterFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            posterFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra("jid", "$customerPhone@s.whatsapp.net") // सीधा ग्राहक को भेजने के लिए
            putExtra(Intent.EXTRA_TEXT, "नमस्ते! हमारी दुकान पर नया ऑफर आया है। कृपया देखें।")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        
        context.startActivity(Intent.createChooser(intent, "Share via WhatsApp"))
    }

    /**
     * पूरे कस्टमर बेस को ब्रॉडकास्ट भेजना (Campaign Manager Blueprint #13)
     */
    fun broadcastToAll(context: Context, posterFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            posterFile
        )
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }
        
        context.startActivity(Intent.createChooser(intent, "Broadcast to All Customers"))
    }
}
