package com.ganesh.hisabkitabpro.domain.businesscard.repository

import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import kotlinx.coroutines.flow.Flow

/**
 * Domain-layer entry point for the procedural business card engine.
 *
 * The repository is *read-only* — there is no card persistence, because every card is
 * computed from the canonical [com.ganesh.hisabkitabpro.domain.model.BusinessProfile]
 * via [com.ganesh.hisabkitabpro.domain.businesscard.MultiTemplateGenerator]. This is
 * the explicit "no data entry, only rendering" architecture the engine targets.
 */
interface BusinessCardRepository {

    /** Hot stream of the sanitised business profile used by the engine. */
    fun observeProfile(): Flow<BusinessCardProfile>

    /** Returns the canonical 50-card master sheet (deterministic for the lifetime of the process). */
    fun generateAll(): List<BusinessCardVariation>
}
