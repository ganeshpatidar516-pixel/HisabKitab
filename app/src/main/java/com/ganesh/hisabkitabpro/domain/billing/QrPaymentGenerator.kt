package com.ganesh.hisabkitabpro.domain.billing

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class QrPaymentGenerator {

    fun generateUpiQr(
        upiId: String,
        name: String,
        amount: Double,
        note: String
    ): Bitmap {

        val upiUrl =
            "upi://pay?pa=$upiId&pn=$name&am=$amount&tn=$note&cu=INR"

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            upiUrl,
            BarcodeFormat.QR_CODE,
            512,
            512
        )

        val bitmap = Bitmap.createBitmap(
            512,
            512,
            Bitmap.Config.RGB_565
        )

        for (x in 0 until 512) {
            for (y in 0 until 512) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) -0x1000000 else -0x1
                )
            }
        }

        return bitmap
    }
}