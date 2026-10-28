package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.AnalyzeImageResponse
import com.clearchain.app.data.remote.dto.UploadImageResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ImageAnalysisApi {

    @Multipart
    @POST("imageanalysis/analyze")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part,
        // "en" or "vi": the language the suggested title and notes come back in
        @Part("language") language: RequestBody
    ): AnalyzeImageResponse

    @Multipart
    @POST("imageanalysis/upload")
    suspend fun uploadFoodImage(
        @Part image: MultipartBody.Part
    ): UploadImageResponse
}
