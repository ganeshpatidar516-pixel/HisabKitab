package com.ganesh.hisabkitabpro.di

import com.ganesh.hisabkitabpro.domain.cloud.SelectiveCloudMirror
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets [com.ganesh.hisabkitabpro.domain.sync.SyncEngine] re-mirror after FastAPI upload success (P2). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncCloudMirrorEntryPoint {
    fun selectiveCloudMirror(): SelectiveCloudMirror
}
