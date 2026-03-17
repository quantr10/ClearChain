package com.clearchain.app.di

import com.clearchain.app.data.remote.api.*
import com.clearchain.app.data.remote.interceptor.AuthInterceptor
import com.clearchain.app.util.Constants
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        json: Json
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideListingApi(retrofit: Retrofit): ListingApi {
        return retrofit.create(ListingApi::class.java)
    }

    @Provides
    @Singleton
    fun providePickupRequestApi(retrofit: Retrofit): PickupRequestApi {
        return retrofit.create(PickupRequestApi::class.java)
    }

    @Provides
    @Singleton
    fun provideAdminApi(retrofit: Retrofit): AdminApi {
        return retrofit.create(AdminApi::class.java)
    }

    @Provides
    @Singleton
    fun provideInventoryApi(retrofit: Retrofit): InventoryApi {
        return retrofit.create(InventoryApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImageAnalysisApi(retrofit: Retrofit): ImageAnalysisApi {
        return retrofit.create(ImageAnalysisApi::class.java)
    }

    // ✅ ADD: Provide OrganizationApi
    @Provides
    @Singleton
    fun provideOrganizationApi(retrofit: Retrofit): OrganizationApi {
        return retrofit.create(OrganizationApi::class.java)
    }
}