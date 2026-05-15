package com.ganesh.hisabkitabpro.domain.ai.marketing

import android.content.Context
import com.ganesh.hisabkitabpro.core.storage.AppStoragePaths
import android.graphics.*
import java.io.File
import java.io.FileOutputStream

data class PosterData(
    val shopName: String,
    val offerTitle: String,
    val items: List<String>,
    val discountText: String,
    val contact: String
)

object PosterGenerator {

    /**
     * दुकान के लिए एक प्रोफेशनल विज्ञापन पोस्टर (Image) बनाता है।
     */
    fun generateOfferPoster(context: Context, data: PosterData): File {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background - Premium Gradient Style
        val paint = Paint()
        val gradient = LinearGradient(0f, 0f, 0f, height.toFloat(), Color.parseColor("#1A237E"), Color.parseColor("#000000"), Shader.TileMode.CLAMP)
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Shop Name
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(data.shopName.uppercase(), width / 2f, 150f, paint)

        // Special Offer Banner
        paint.color = Color.parseColor("#FFD600")
        paint.textSize = 100f
        canvas.drawText(data.offerTitle, width / 2f, 350f, paint)

        // Items List
        paint.color = Color.WHITE
        paint.textSize = 50f
        paint.isFakeBoldText = false
        var y = 550f
        data.items.forEach { item ->
            canvas.drawText("• $item", width / 2f, y, paint)
            y += 80f
        }

        // Discount Tag
        paint.color = Color.parseColor("#D32F2F")
        canvas.drawRect(width / 4f, y + 50f, 3 * width / 4f, y + 200f, paint)
        paint.color = Color.WHITE
        paint.textSize = 70f
        paint.isFakeBoldText = true
        canvas.drawText(data.discountText, width / 2f, y + 150f, paint)

        // Contact Info
        paint.textSize = 40f
        paint.color = Color.LTGRAY
        canvas.drawText("Call: ${data.contact}", width / 2f, height - 100f, paint)

        val file = File(AppStoragePaths.exportsCacheDir(context), "Poster_${System.currentTimeMillis()}.png")
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
        return file
    }
}
