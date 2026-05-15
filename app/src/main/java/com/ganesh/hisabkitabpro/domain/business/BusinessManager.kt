package com.ganesh.hisabkitabpro.domain.business

data class Business(
    val id: String,
    val name: String,
    val type: String,
    val gstNumber: String? = null,
    val address: String? = null,
    val logoUrl: String? = null
)

class BusinessManager {
    private val businesses = mutableListOf<Business>()
    private var activeBusinessId: String? = null

    fun addBusiness(business: Business) {
        businesses.add(business)
        if (activeBusinessId == null) {
            activeBusinessId = business.id
        }
    }

    fun getActiveBusiness(): Business? {
        return businesses.find { it.id == activeBusinessId }
    }

    fun switchBusiness(businessId: String) {
        activeBusinessId = businessId
    }

    fun getAllBusinesses(): List<Business> {
        return businesses.toList()
    }
}
