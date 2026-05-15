package com.ganesh.hisabkitabpro.domain.automation

import android.content.Context
import com.ganesh.hisabkitabpro.utils.SmsSender
import com.ganesh.hisabkitabpro.utils.WhatsAppSender

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * Has zero callers in `app/src/main`. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt:48273`).
 *
 * This object would auto-send SMS + WhatsApp without explicit user confirmation,
 * which is incompatible with the project's user-consent guarantee for outgoing
 * messages. The live reminder flow always opens an intent-composer the user must
 * tap Send on; never auto-dispatches.
 *
 * Kept only as a reference for the AI-driven reminder feature design. Any future
 * implementation must route through the live BulkReminderService + WhatsAppBillSender
 * intent flow, not this object.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Auto-send messaging helper. Violates the user-confirmation guarantee. " +
        "Use BulkReminderService + WhatsAppBillSender (intent-only) instead.",
    level = DeprecationLevel.WARNING
)
object AiMessageAutomation {

    fun sendLedgerToCustomer(

        context: Context,

        phone: String,

        ledgerText: String

    ) {

        // Send SMS automatically
        SmsSender.sendSms(

            phone,
            ledgerText
        )

        // Open WhatsApp message
        WhatsAppSender.sendMessage(

            context,
            phone,
            ledgerText
        )
    }
}