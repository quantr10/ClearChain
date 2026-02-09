package com.clearchain.app.di

import com.clearchain.app.data.repository.AuthRepositoryImpl
import com.clearchain.app.data.repository.ListingRepositoryImpl
import com.clearchain.app.domain.repository.AuthRepository
import com.clearchain.app.domain.repository.ListingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindListingRepository(
        listingRepositoryImpl: ListingRepositoryImpl
    ): ListingRepository
}