package com.ganesh.hisabkitabpro.domain.ai.intelligence

data class AIResponse(
    val answer: String,
    val source: String, // internal / external / hybrid
    val confidence: Double,
    val analysisType: String // business / knowledge / prediction
)
