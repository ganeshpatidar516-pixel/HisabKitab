package com.ganesh.hisabkitabpro.network.api

import retrofit2.http.GET

data class DashboardAnalytics(
    val totalCredit: Double,
    val totalDebit: Double,
    val netBalance: Double,
    val customerCount: Int,
    val transactionCount: Int,
    val riskSummary: Map<String, Int>
)

interface AnalyticsApi {
    @GET("analytics/dashboard")
    suspend fun getDashboardAnalytics(): DashboardAnalytics
}