package com.ganesh.hisabkitabpro.domain.invoice

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QrCodeGenerator {

    /**
     * UPI पेमेंट के लिए QR कोड बनाता है।
     * फॉर्मेट: upi://pay?pa=VPA&pn=NAME&am=AMOUNT&cu=INR
     */
    fun generateUpiQr(vpa: String, name: String, amount: Double): Bitmap? {
        val upiUrl = "upi://pay?pa=$vpa&pn=$name&am=$amount&cu=INR"
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                upiUrl,
                BarcodeFormat.QR_CODE,
                512,
                512
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}