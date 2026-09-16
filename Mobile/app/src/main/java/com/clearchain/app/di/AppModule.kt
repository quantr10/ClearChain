package com.clearchain.app.di

import android.content.Context
import androidx.room.Room
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.local.database.MIGRATION_7_8
import com.clearchain.app.data.local.database.MIGRATION_8_9
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
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration() // last resort for versions without an explicit path
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
    fun provideListingDao(database: ClearChainDatabase) = database.listingDao()

    @Provides
    @Singleton
    fun providePickupRequestDao(database: ClearChainDatabase) = database.pickupRequestDao()

    @Provides
    @Singleton
    fun provideInventoryDao(database: ClearChainDatabase) = database.inventoryDao()

    @Provides
    @Singleton
    fun provideNotificationDao(database: ClearChainDatabase) = database.notificationDao()

    @Provides
    @Singleton
    fun provideSettingsStore(
        @ApplicationContext context: Context
    ) = com.clearchain.app.data.local.SettingsStore(context)
}
