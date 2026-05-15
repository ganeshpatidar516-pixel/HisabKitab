package com.ganesh.hisabkitabpro.domain.service

import com.ganesh.hisabkitabpro.domain.bi.GrowthForecastEngine
import com.ganesh.hisabkitabpro.domain.bi.RiskProbabilityEngine
import com.ganesh.hisabkitabpro.domain.model.Customer
import com.ganesh.hisabkitabpro.domain.model.Transaction

object BusinessIntelligenceService {

    /**
     * [Block 5: Service Engine Layer]
     * पूरे बिज़नेस का स्वास्थ्य (Health) चेक करता है।
     */
    fun getBusinessHealthReport(transactions: List<Transaction>, customers: List<Customer>): Map<String, Any> {
        val nextMonthSales = GrowthForecastEngine.forecastNextMonthSales(transactions)
        val highRiskCustomers = customers.filter { customer ->
            val customerTx = transactions.filter { it.customerId == customer.id }
            RiskProbabilityEngine.calculateRiskScore(customer, customerTx) > 0.7
        }

        return mapOf(
            "predicted_sales" to nextMonthSales,
            "risky_customers_count" to highRiskCustomers.size,
            "health_score" to if (highRiskCustomers.isEmpty()) "Excellent" else "Attention Required"
        )
    }
}
