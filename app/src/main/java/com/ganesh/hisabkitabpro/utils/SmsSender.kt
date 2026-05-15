package com.ganesh.hisabkitabpro.utils

import android.content.Context
import android.telephony.SmsManager

/**
 * LEGACY / DEAD-PAIRED — DO NOT USE FOR NEW CODE.
 *
 * The only consumer of this object is the unreachable
 * [com.ganesh.hisabkitabpro.domain.automation.AiMessageAutomation], which is
 * itself never invoked. Confirmed stripped from release AABs by R8
 * (see `app/build/outputs/mapping/release/usage.txt`).
 *
 * The live SMS reminder pipeline does NOT route through this class — it goes
 * via the dedicated `BulkReminderService` + intent-based composer flow that
 * never silently auto-sends. Keeping that user-confirmation guarantee is why
 * this auto-send helper must stay deprecated.
 */
@Deprecated(
    message = "Legacy auto-send SMS helper. The live pipeline uses BulkReminderService + " +
        "intent-based composer (user-confirmed). Do not call.",
    level = DeprecationLevel.WARNING
)
object SmsSender {

    fun sendSms(

        phone: String,

        message: String

    ) {

        val smsManager = SmsManager.getDefault()

        smsManager.sendTextMessage(

            phone,
            null,
            message,
            null,
            null
        )
    }
}