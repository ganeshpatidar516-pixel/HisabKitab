package com.ganesh.hisabkitabpro.domain.bi

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

object GrowthForecastEngine {

    /**
     * पिछले ट्रांजैक्शन्स के आधार पर अगले महीने की सेल का अनुमान (Forecast) लगाता है।
     */
    fun forecastNextMonthSales(transactions: List<Transaction>): Double {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        val recentSales = transactions
            .filter { it.createdAt >= thirtyDaysAgo && it.type == TransactionType.CREDIT }
            .sumOf { it.amount }
            
        // सरल अनुमान: पिछले महीने की सेल का 1.1x (10% संभावित ग्रोथ)
        return recentSales * 1.1
    }
}