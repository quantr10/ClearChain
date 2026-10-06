package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.DisputeListItemResponse
import com.clearchain.app.data.remote.dto.DisputeListResponse
import com.clearchain.app.data.remote.dto.DisputeResponse
import com.clearchain.app.data.remote.dto.MyDisputeListResponse
import com.clearchain.app.data.remote.dto.ResolveDisputeRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface DisputeApi {

    @Multipart
    @POST("disputes")
    suspend fun openDispute(
        @Part("pickupRequestId") pickupRequestId: RequestBody,
        @Part("reason") reason: RequestBody,
        @Part("statement") statement: RequestBody?,
        @Part photo: MultipartBody.Part?
    ): DisputeResponse

    // The NGO's own disputes, used to tell whether a pickup has already been reported.
    @GET("disputes/mine")
    suspend fun getMyDisputes(): MyDisputeListResponse

    // Admin only — the server enforces the role check.
    @GET("disputes")
    suspend fun getDisputes(
        @Query("status") status: String? = null,
        @Query("pickupRequestId") pickupRequestId: String? = null
    ): DisputeListResponse

    // Admin only — moves an open dispute to under_review.
    @PUT("disputes/{id}/review")
    suspend fun startReview(@Path("id") id: String): DisputeListItemResponse

    @PUT("disputes/{id}/resolve")
    suspend fun resolveDispute(
        @Path("id") id: String,
        @Body request: ResolveDisputeRequest
    ): DisputeListItemResponse
}
