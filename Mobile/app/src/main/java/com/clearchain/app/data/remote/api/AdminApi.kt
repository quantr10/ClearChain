package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.*
import retrofit2.http.*

interface AdminApi {

    @GET("admin/organizations")
    suspend fun getAllOrganizations(
        @Query("type") type: String? = null,
        @Query("verified") verified: Boolean? = null
    ): OrganizationListResponse

    @PUT("admin/organizations/{id}/verify")
    suspend fun verifyOrganization(
        @Path("id") id: String
    ): OrganizationResponse

    @PUT("admin/organizations/{id}/unverify")
    suspend fun unverifyOrganization(
        @Path("id") id: String
    ): OrganizationResponse

    @GET("admin/statistics/overview")
    suspend fun getStatistics(): AdminStatsResponse

    @GET("admin/statistics")
    suspend fun getDetailedStatistics(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("preset") preset: String = "all"
    ): AdminDetailedStatsResponse

    @GET("admin/health")
    suspend fun getSystemHealth(): AdminHealthResponse

    @GET("admin/alerts")
    suspend fun getAlertFeed(): AdminAlertFeedResponse

    @GET("admin/pickuprequests")
    suspend fun getAllPickupRequests(): PickupRequestsResponse

    @GET("admin/user-growth")
    suspend fun getUserGrowth(
        @Query("days") days: Int = 30
    ): UserGrowthResponse
}