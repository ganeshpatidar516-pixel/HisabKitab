package com.ganesh.hisabkitabpro.domain.model

data class TransactionInput(

    val customerName: String,

    val amount: Double,

    val type: TransactionType,

    val note: String? = null

)