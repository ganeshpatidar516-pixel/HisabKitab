package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.core.feature.FeatureRecoveryManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FeatureRecoveryEntryPoint {
    fun featureRecoveryManager(): FeatureRecoveryManager
}
