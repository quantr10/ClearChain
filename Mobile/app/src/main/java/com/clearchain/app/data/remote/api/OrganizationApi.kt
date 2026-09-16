package com.clearchain.app.data.remote.api

import android.annotation.SuppressLint
import com.clearchain.app.data.remote.dto.ActivityResponse
import com.clearchain.app.data.remote.dto.AvatarUploadResponse
import com.clearchain.app.data.remote.dto.DocumentUploadResponse
import com.clearchain.app.data.remote.dto.DashboardStatsResponse
import com.clearchain.app.data.remote.dto.TodaySummaryResponse
import com.clearchain.app.data.remote.dto.UpdateProfileRequest
import com.clearchain.app.data.remote.dto.MessageResponse
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.*

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PublicProfileData(
    val id: String,
    val name: String,
    val type: String,
    val email: String? = null,
    val location: String? = null,
    val address: String? = null,
    val state: String? = null,
    val zipCode: String? = null,
    val phone: String? = null,
    val description: String? = null,
    val hours: String? = null,
    val profilePictureUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val contactPerson: String? = null,
    val verified: Boolean = false,
    val verificationStatus: String = "pending",
    val createdAt: String,
    val averageRating: Double = 0.0,
    val reviewCount: Int = 0,
    val completedPickups: Int = 0,
    // Weighed and converted by the API (QuantityUnits), like my/stats - so an
    // organization's public impact matches what it sees on its own dashboard.
    val foodSaved: Int = 0,
    val mealsEstimate: Int = 0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class PublicProfileResponse(val data: PublicProfileData)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NgoReputationData(
    val totalRequests: Int = 0,
    val completedPickups: Int = 0,
    val cancelledPickups: Int = 0,
    val completionRate: Double = 0.0
)

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class NgoReputationResponse(val data: NgoReputationData)

interface OrganizationApi {

    @GET("organizations/my/stats")
    suspend fun getMyStats(): DashboardStatsResponse

    // days: activity window. 7 (default) matches the dashboard sparkline; analytics passes a wider window.
    @GET("organizations/my/activity")
    suspend fun getMyActivity(@Query("days") days: Int = 7): ActivityResponse

    @GET("organizations/my/today-summary")
    suspend fun getTodaySummary(): TodaySummaryResponse

    @PUT("organizations/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): MessageResponse

    @GET("organizations/{id}/public")
    suspend fun getPublicProfile(@Path("id") id: String): PublicProfileResponse

    @GET("organizations/{id}/reputation")
    suspend fun getNgoReputation(@Path("id") id: String): NgoReputationResponse

    @Multipart
    @POST("organizations/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): AvatarUploadResponse

    @Multipart
    @POST("organizations/documents")
    suspend fun uploadDocument(
        @Part document: MultipartBody.Part
    ): DocumentUploadResponse
}