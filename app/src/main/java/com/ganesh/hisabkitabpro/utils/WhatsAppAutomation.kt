package com.ganesh.hisabkitabpro.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * Has zero callers in `app/src/main`. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt:49803`).
 *
 * The LIVE WhatsApp share path is [com.ganesh.hisabkitabpro.util.WhatsAppBillSender]
 * (singular `util` package). That class supports both whatsapp + whatsapp-business,
 * handles missing-app fallback gracefully, and is wired into the live reminder /
 * bill-share flow.
 */
@Deprecated(
    message = "Legacy WhatsApp share helper. Use com.ganesh.hisabkitabpro.util.WhatsAppBillSender (live).",
    replaceWith = ReplaceWith("com.ganesh.hisabkitabpro.util.WhatsAppBillSender"),
    level = DeprecationLevel.WARNING
)
object WhatsAppAutomation {
    fun shareToWhatsApp(context: Context, phoneNumber: String?, message: String) {
        try {
            val uri = if (phoneNumber.isNullOrBlank()) {
                Uri.parse("whatsapp://send?text=${Uri.encode(message)}")
            } else {
                val cleanNumber = phoneNumber.replace("+", "").replace(" ", "")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(message)}")
            }
            
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.whatsapp")
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            // Fallback to general share
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, message)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
        }
    }
}
