package com.clearchain.app.di

import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.remote.signalr.SignalRService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SignalRModule {

    @Provides
    @Singleton
    fun provideSignalRService(
        database: ClearChainDatabase
    ): SignalRService {
        return SignalRService(database)
    }
}