package com.ganesh.hisabkitabpro.domain.payment

import android.net.Uri

/**
 * Builds standard `upi://pay` deep links for any UPI app on the device.
 * [amountRupee] should be decimal string e.g. `"150.00"` or `"150"`.
 */
object UpiIntentBuilder {

    fun buildPayUri(
        payeeVpa: String,
        payeeName: String,
        amountRupee: String,
        transactionNote: String,
        txnRef: String
    ): Uri {
        return Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", payeeVpa.trim())
            .appendQueryParameter("pn", payeeName.trim().ifBlank { "Payee" })
            .appendQueryParameter("am", amountRupee.trim())
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", transactionNote.take(80))
            .appendQueryParameter("tr", txnRef.take(35))
            .build()
    }
}
