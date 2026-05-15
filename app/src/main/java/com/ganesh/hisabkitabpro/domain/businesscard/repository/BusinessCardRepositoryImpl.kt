package com.ganesh.hisabkitabpro.domain.businesscard.repository

import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardProfile
import com.ganesh.hisabkitabpro.domain.businesscard.BusinessCardVariation
import com.ganesh.hisabkitabpro.domain.businesscard.MultiTemplateGenerator
import com.ganesh.hisabkitabpro.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessCardRepositoryImpl @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : BusinessCardRepository {

    private val cachedSheet: List<BusinessCardVariation> by lazy { MultiTemplateGenerator.generate() }

    override fun observeProfile(): Flow<BusinessCardProfile> =
        settingsRepository.getBusinessProfile().map { BusinessCardProfile.from(it) }

    override fun generateAll(): List<BusinessCardVariation> = cachedSheet
}
