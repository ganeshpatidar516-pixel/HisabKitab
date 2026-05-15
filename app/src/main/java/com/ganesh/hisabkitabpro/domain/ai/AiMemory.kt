package com.ganesh.hisabkitabpro.domain.ai

data class AiMemory(

    val commandHistory: MutableList<String> = mutableListOf(),

    val customerPatterns: MutableMap<String, Int> = mutableMapOf(),

    val frequentActions: MutableMap<String, Int> = mutableMapOf()

)