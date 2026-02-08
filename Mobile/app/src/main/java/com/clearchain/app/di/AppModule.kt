package com.clearchain.app.di

import android.content.Context
import androidx.room.Room
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.util.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ClearChainDatabase {
        return Room.databaseBuilder(
            context,
            ClearChainDatabase::class.java,
            ClearChainDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: ClearChainDatabase) = database.userDao()

    @Provides
    @Singleton
    fun provideAuthTokenDao(database: ClearChainDatabase) = database.authTokenDao()

    @Provides
    @Singleton
    fun provideNetworkUtils(
        @ApplicationContext context: Context
    ): NetworkUtils {
        return NetworkUtils(context)
    }
}