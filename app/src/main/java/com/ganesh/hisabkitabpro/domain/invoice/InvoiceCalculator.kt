package com.ganesh.hisabkitabpro.domain.invoice

import com.ganesh.hisabkitabpro.domain.model.InvoiceItem

object InvoiceCalculator {
    fun calculateSubtotal(items: List<InvoiceItem>): Double {
        return items.sumOf { it.total }
    }

    /**
     * Calculates GST amount based on blueprint logic.
     * if gst_enabled == True: subtotal * gst_rate
     * else: 0
     */
    fun calculateGst(subtotal: Double, gstPercentage: Double, isEnabled: Boolean): Double {
        return if (isEnabled) {
            (subtotal * gstPercentage) / 100
        } else {
            0.0
        }
    }

    fun calculateFinalAmount(subtotal: Double, gstAmount: Double, discount: Double): Double {
        return subtotal + gstAmount - discount
    }
}