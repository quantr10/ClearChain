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

    @GET("admin/statistics")
    suspend fun getStatistics(): AdminStatsResponse

    @GET("admin/pickuprequests")
    suspend fun getAllPickupRequests(): PickupRequestsResponse
}