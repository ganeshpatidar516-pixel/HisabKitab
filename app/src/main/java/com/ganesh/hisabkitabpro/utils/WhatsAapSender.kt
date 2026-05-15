package com.ganesh.hisabkitabpro.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * Note: this file is misnamed (`WhatsAapSender.kt` — typo) and lives only because
 * its single consumer ([com.ganesh.hisabkitabpro.domain.automation.AiMessageAutomation])
 * still references it. Both are unreachable in the live navigation graph and
 * are stripped from release AABs by R8 (see
 * `app/build/outputs/mapping/release/usage.txt`).
 *
 * The LIVE WhatsApp share path goes through
 * [com.ganesh.hisabkitabpro.util.WhatsAppBillSender] (singular `util` package),
 * which is intent-only, requires the user to tap Send, and never auto-sends.
 */
@Deprecated(
    message = "Legacy WhatsApp helper. Use com.ganesh.hisabkitabpro.util.WhatsAppBillSender for the live share flow.",
    replaceWith = ReplaceWith("com.ganesh.hisabkitabpro.util.WhatsAppBillSender"),
    level = DeprecationLevel.WARNING
)
object WhatsAppSender {

    fun sendMessage(

        context: Context,

        phoneNumber: String,

        message: String

    ) {

        val uri =
            "https://wa.me/$phoneNumber?text=" +
                    Uri.encode(message)

        val intent = Intent(Intent.ACTION_VIEW)

        intent.data = Uri.parse(uri)

        context.startActivity(intent)
    }
}