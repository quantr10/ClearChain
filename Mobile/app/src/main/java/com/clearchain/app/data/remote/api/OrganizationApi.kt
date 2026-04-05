package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.DashboardStatsResponse
import com.clearchain.app.data.remote.dto.UpdateProfileRequest
import com.clearchain.app.data.remote.dto.MessageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface OrganizationApi {

    @GET("organizations/my/stats")
    suspend fun getMyStats(): DashboardStatsResponse

    @PUT("organizations/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): MessageResponse
}