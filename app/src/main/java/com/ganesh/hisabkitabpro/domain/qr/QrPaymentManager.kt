package com.ganesh.hisabkitabpro.domain.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.util.*

data class UpiPaymentDetails(
    val upiId: String,
    val name: String,
    val amount: String? = null,
    val note: String? = null,
    val transactionId: String = UUID.randomUUID().toString()
)

object QrPaymentManager {

    /**
     * ULTRA PRO MAX QR Generator
     * upi://pay?pa={upi_id}&pn={name}&am={amount}&tn={note}&tr={trans_id}
     */
    fun generateDynamicUpiQr(details: UpiPaymentDetails, size: Int = 512): Bitmap? {
        val upiString = StringBuilder("upi://pay?")
            .append("pa=${details.upiId}")
            .append("&pn=${UriEncode(details.name)}")
            
        details.amount?.let { upiString.append("&am=$it") }
        details.note?.let { upiString.append("&tn=${UriEncode(it)}") }
        upiString.append("&tr=${details.transactionId}")

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(upiString.toString(), BarcodeFormat.QR_CODE, size, size, hints)
            
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun UriEncode(text: String): String {
        return java.net.URLEncoder.encode(text, "UTF-8")
    }

    /**
     * AI Payment Parser
     * Decodes UPI String into Structured Data
     */
    fun parseUpiString(upiUri: String): UpiPaymentDetails? {
        if (!upiUri.startsWith("upi://pay")) return null
        
        val uri = upiUri.substringAfter("?")
        val params = uri.split("&").associate { 
            val parts = it.split("=")
            parts[0] to (parts.getOrNull(1) ?: "")
        }

        val upiId = params["pa"] ?: return null
        val name = params["pn"]?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Unknown"
        val amount = params["am"]
        val note = params["tn"]?.let { java.net.URLDecoder.decode(it, "UTF-8") }

        return UpiPaymentDetails(
            upiId = upiId,
            name = name,
            amount = amount,
            note = note,
            transactionId = params["tr"] ?: ""
        )
    }
}
