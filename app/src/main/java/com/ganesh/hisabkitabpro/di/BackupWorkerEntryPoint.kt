package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.domain.backup.CloudBackupManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupWorkerEntryPoint {
    fun cloudBackupManager(): CloudBackupManager
}
