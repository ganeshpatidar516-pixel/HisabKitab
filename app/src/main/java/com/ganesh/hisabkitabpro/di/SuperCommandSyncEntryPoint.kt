package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.commandos.SuperCommandService
import com.ganesh.hisabkitabpro.commandos.sync.OfflineCommandJournal
import com.ganesh.hisabkitabpro.commandos.sync.QueueHealthMetrics
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SuperCommandSyncEntryPoint {
    fun superCommandService(): SuperCommandService
    fun offlineCommandJournal(): OfflineCommandJournal
    fun queueHealthMetrics(): QueueHealthMetrics
}
