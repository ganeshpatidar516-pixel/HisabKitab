package com.ganesh.hisabkitabpro.di

import android.content.Context
import com.ganesh.hisabkitabpro.core.analytics.CrashAnalytics
import com.ganesh.hisabkitabpro.core.background.TaskManager
import com.ganesh.hisabkitabpro.core.crash.GlobalCrashHandler
import com.ganesh.hisabkitabpro.core.feature.FeatureRecoveryManager
import com.ganesh.hisabkitabpro.core.performance.PerformanceGuard
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideGlobalCrashHandler(@ApplicationContext context: Context): GlobalCrashHandler {
        return GlobalCrashHandler(context)
    }

    @Provides
    @Singleton
    fun provideFeatureRecoveryManager(@ApplicationContext context: Context): FeatureRecoveryManager {
        return FeatureRecoveryManager(context)
    }

    @Provides
    @Singleton
    fun provideTaskManager(@ApplicationContext context: Context): TaskManager {
        return TaskManager(context)
    }

    @Provides
    @Singleton
    fun providePerformanceGuard(@ApplicationContext context: Context): PerformanceGuard {
        return PerformanceGuard(context)
    }

    @Provides
    @Singleton
    fun provideCrashAnalytics(): CrashAnalytics {
        return CrashAnalytics()
    }
}
