package com.clearchain.app.data.remote.api

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
}
