package com.ganesh.hisabkitabpro.domain.ai.marketing

import android.graphics.Bitmap
import com.ganesh.hisabkitabpro.domain.ai.AIAction
import com.ganesh.hisabkitabpro.domain.ai.AIResponse
import com.ganesh.hisabkitabpro.domain.ai.ResponseType

data class VideoMetadata(
    val title: String,
    val durationSec: Int,
    val frames: List<Bitmap> = emptyList(),
    val musicUri: String? = null
)

object VideoTemplateEngine {

    /**
     * विज्ञापन वीडियो के लिए स्ट्रक्चर तैयार करता है।
     */
    fun preparePromoVideo(shopName: String, offer: String): VideoMetadata {
        return VideoMetadata(
            title = "$shopName - $offer Promo",
            durationSec = 15 // Default 15 seconds
        )
    }

    /**
     * AI को वीडियो बनाने का कमांड देना।
     */
    fun generateVideoResponse(shopName: String): AIResponse {
        return AIResponse(
            message = "मैंने $shopName के लिए एक प्रोमोशनल वीडियो ड्राफ्ट तैयार कर लिया है।",
            type = ResponseType.ADVERTISEMENT,
            actions = listOf(
                AIAction("वीडियो देखें (Preview)", "video_preview/$shopName"),
                AIAction("शेयर करें (Share)", "share_video")
            )
        )
    }
}
