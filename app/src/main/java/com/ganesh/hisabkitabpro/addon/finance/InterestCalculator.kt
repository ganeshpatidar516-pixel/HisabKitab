package com.ganesh.hisabkitabpro.addon.finance

/**
 * Stand-alone interest math — does not persist or alter principal in DB.
 * UI / reports can call this for display-only overlays.
 */
object InterestCalculator {

    /** Simple flat interest: principal * (annualRatePercent/100) * (days/365). */
    fun flatAnnualInterest(
        principalPaise: Long,
        annualRatePercent: Double,
        days: Int
    ): Long {
        if (principalPaise <= 0 || annualRatePercent <= 0 || days <= 0) return 0L
        val rupee = principalPaise / 100.0
        val interestRupee = rupee * (annualRatePercent / 100.0) * (days / 365.0)
        return (interestRupee * 100.0).toLong()
    }
}
