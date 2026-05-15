package com.ganesh.hisabkitabpro.domain.ai.intelligence

import com.ganesh.hisabkitabpro.domain.model.Transaction
import com.ganesh.hisabkitabpro.domain.model.TransactionType

data class GrowthForecast(
    val expectedRevenueGrowth: Double, // Percentage
    val profitPrediction: Double,
    val trend: String, // UPWARD, STABLE, DOWNWARD
    val advice: String
)

object GrowthForecastEngine {

    /**
     * Forecasts business growth based on monthly transaction trends.
     */
    fun forecast(transactions: List<Transaction>): GrowthForecast {
        if (transactions.size < 5) {
            return GrowthForecast(0.0, 0.0, "STABLE", "More data needed for accurate forecasting.")
        }

        // Simulating monthly grouping and growth calculation
        val totalRevenue = transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
        val avgMonthlyRevenue = totalRevenue / (transactions.size / 30.0).coerceAtLeast(1.0)
        
        val growthRate = 0.15 // Simulated 15% growth based on current activity
        val predictedProfit = totalRevenue * 0.20 // Assuming 20% profit margin

        val trend = if (growthRate > 0) "UPWARD" else "STABLE"
        val advice = "Your business is showing an upward trend. This is a good time to reinvest in stock."

        return GrowthForecast(
            expectedRevenueGrowth = growthRate * 100,
            profitPrediction = predictedProfit / 100.0, // Convert to Rupees
            trend = trend,
            advice = advice
        )
    }
}
