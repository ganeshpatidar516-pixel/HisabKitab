package com.ganesh.hisabkitabpro.domain.ocr

data class ParsedBill(

    val items: List<String> = emptyList(),

    val totalAmount: Double? = null,

    val detectedCustomer: String? = null

)

object OcrBillParser {

    fun parseText(
        text: String
    ): ParsedBill {

        val lines = text.lines()

        val items = mutableListOf<String>()

        var total: Double? = null

        lines.forEach { line ->

            if (line.contains("total", true)) {

                val number =
                    Regex("\\d+(\\.\\d+)?")
                        .find(line)

                total =
                    number?.value?.toDoubleOrNull()

            } else {

                if (line.length > 3) {
                    items.add(line)
                }
            }
        }

        return ParsedBill(

            items = items,

            totalAmount = total
        )
    }
}