package com.ganesh.hisabkitabpro.domain.search

import com.ganesh.hisabkitabpro.domain.model.Customer

object CustomerSearchEngine {

    /**
     * Filters customers based on search query.
     */
    fun search(
        customers: List<Customer>,
        query: String
    ): List<Customer> {
        if (query.isBlank()) return customers

        val lowerQuery = query.lowercase().trim()
        
        return customers.filter { customer ->
            customer.name.lowercase().contains(lowerQuery) ||
            (customer.phone?.contains(lowerQuery) ?: false)
        }
    }
}
