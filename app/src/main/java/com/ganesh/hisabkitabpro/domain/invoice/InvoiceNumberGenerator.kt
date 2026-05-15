package com.ganesh.hisabkitabpro.domain.invoice

import java.text.SimpleDateFormat
import java.util.*

object InvoiceNumberGenerator {
    /**
     * यूनिक इनवॉइस नंबर जनरेट करता है। फॉर्मेट: INV-YYYYMMDD-HHMM
     */
    fun generateInvoiceNumber(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        return "INV-$timeStamp"
    }
}