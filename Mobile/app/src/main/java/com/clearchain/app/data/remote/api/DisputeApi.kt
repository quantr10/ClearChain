package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.AddStatementRequest
import com.clearchain.app.data.remote.dto.DisputeListResponse
import com.clearchain.app.data.remote.dto.DisputeResponse
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

    @GET("disputes/{id}")
    suspend fun getDispute(@Path("id") id: String): DisputeResponse

    @GET("disputes/my")
    suspend fun getMyDisputes(): DisputeListResponse

    @PUT("disputes/{id}/grocery-statement")
    suspend fun addGroceryStatement(
        @Path("id") id: String,
        @Body request: AddStatementRequest
    ): DisputeResponse
}
