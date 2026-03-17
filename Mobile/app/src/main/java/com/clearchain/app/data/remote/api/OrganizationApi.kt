package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.UpdateProfileRequest
import com.clearchain.app.data.remote.dto.MessageResponse
import retrofit2.http.Body
import retrofit2.http.PUT

interface OrganizationApi {
    
    @PUT("organizations/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): MessageResponse
}