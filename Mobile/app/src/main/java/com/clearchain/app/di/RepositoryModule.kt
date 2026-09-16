package com.clearchain.app.di

import com.clearchain.app.data.repository.*
import com.clearchain.app.domain.repository.*
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

    @Binds
    @Singleton
    abstract fun bindPickupRequestRepository(
        pickupRequestRepositoryImpl: PickupRequestRepositoryImpl
    ): PickupRequestRepository

    @Binds
    @Singleton
    abstract fun bindOrganizationRepository(
        organizationRepositoryImpl: OrganizationRepositoryImpl
    ): OrganizationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}
