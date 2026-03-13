package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.AnalyzeImageResponse
import com.clearchain.app.data.remote.dto.UploadImageResponse
import com.clearchain.app.data.remote.dto.FoodAnalysisData
import okhttp3.MultipartBody
import retrofit2.http.*

interface ImageAnalysisApi {
    
    @Multipart
    @POST("imageanalysis/analyze")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part
    ): AnalyzeImageResponse
    
    // ✅ NEW: Save analysis after listing created
    @POST("imageanalysis/save")
    suspend fun saveAnalysis(
        @Body analysisData: FoodAnalysisData
    )

    @Multipart
    @POST("imageanalysis/upload")
    suspend fun uploadFoodImage(
        @Part image: MultipartBody.Part
    ): UploadImageResponse
}