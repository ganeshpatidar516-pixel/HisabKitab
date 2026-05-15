package com.ganesh.hisabkitabpro.domain.payment

import android.net.Uri

object UPIQRGenerator {

    /**
     * Generates a UPI deep-link URI for payment.
     * Example: upi://pay?pa=upiid@bank&pn=BusinessName&am=Amount&cu=INR
     */
    fun generateUPIString(
        upiId: String,
        businessName: String,
        amount: Double,
        transactionNote: String? = null
    ): String {
        val builder = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", businessName)
            .appendQueryParameter("am", amount.toString())
            .appendQueryParameter("cu", "INR")
        
        if (transactionNote != null) {
            builder.appendQueryParameter("tn", transactionNote)
        }

        return builder.build().toString()
    }
}
