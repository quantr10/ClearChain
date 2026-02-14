package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.*
import retrofit2.http.*

interface PickupRequestApi {

    @POST("pickuprequests")
    suspend fun createPickupRequest(
        @Body request: CreatePickupRequestRequest
    ): PickupRequestResponse

    @GET("pickuprequests/ngo/my")
    suspend fun getMyPickupRequests(): PickupRequestsResponse

    @GET("pickuprequests/grocery/my")
    suspend fun getGroceryPickupRequests(): PickupRequestsResponse

    @GET("pickuprequests/{id}")
    suspend fun getPickupRequestById(
        @Path("id") id: String
    ): PickupRequestResponse

    @PUT("pickuprequests/{id}/status")
    suspend fun updatePickupRequestStatus(
        @Path("id") id: String,
        @Body request: UpdatePickupRequestStatusRequest
    ): PickupRequestResponse

    @DELETE("pickuprequests/{id}")
    suspend fun cancelPickupRequest(
        @Path("id") id: String
    ): PickupRequestResponse

     @PUT("pickuprequests/{id}/approve")
    suspend fun approvePickupRequest(
        @Path("id") id: String
    ): PickupRequestResponse

    @PUT("pickuprequests/{id}/reject")
    suspend fun rejectPickupRequest(
        @Path("id") id: String
    ): PickupRequestResponse

    @PUT("pickuprequests/{id}/ready")
    suspend fun markReadyForPickup(
        @Path("id") id: String
    ): PickupRequestResponse

    @PUT("pickuprequests/{id}/picked-up")
    suspend fun markPickedUp(
        @Path("id") id: String
    ): PickupRequestResponse
}