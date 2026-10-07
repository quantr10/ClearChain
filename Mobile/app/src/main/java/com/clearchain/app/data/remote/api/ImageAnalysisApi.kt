package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.AnalyzeImageResponse
import com.clearchain.app.data.remote.dto.UploadImageResponse
import okhttp3.MultipartBody
import retrofit2.http.*

interface ImageAnalysisApi {

    @Multipart
    @POST("imageanalysis/analyze")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part
    ): AnalyzeImageResponse

    @Multipart
    @POST("imageanalysis/upload")
    suspend fun uploadFoodImage(
        @Part image: MultipartBody.Part
    ): UploadImageResponse
}
