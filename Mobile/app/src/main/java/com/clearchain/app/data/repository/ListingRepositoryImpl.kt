package com.clearchain.app.data.repository

import android.content.Context
import android.net.Uri
import com.clearchain.app.data.remote.api.ImageAnalysisApi
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.dto.CreateListingRequest
import com.clearchain.app.data.remote.dto.FoodAnalysisData
import com.clearchain.app.data.remote.dto.UpdateListingQuantityRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ListingRepositoryImpl @Inject constructor(
    private val listingApi: ListingApi,
    private val imageAnalysisApi: ImageAnalysisApi,
    @ApplicationContext private val context: Context
) : ListingRepository {

    override suspend fun createListing(
        title: String, description: String, category: String,
        quantity: Int, unit: String, expiryDate: String,
        pickupTimeStart: String, pickupTimeEnd: String, imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.createListing(
                CreateListingRequest(title, description, category, quantity, unit,
                    expiryDate, pickupTimeStart, pickupTimeEnd, imageUrl)
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getMyListings(): Result<List<Listing>> {
        return try {
            val response = listingApi.getMyListings()
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) { Result.failure(e) }
    }

    // ═══ UPDATED: Pass lat/lng/radiusKm to API (Part 2) ═══
    override suspend fun getAllListings(
        status: String?, category: String?,
        lat: Double?, lng: Double?, radiusKm: Int?
    ): Result<List<Listing>> {
        return try {
            val response = listingApi.getAllListings(status, category, lat, lng, radiusKm)
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getListingById(id: String): Result<Listing> {
        return try {
            val response = listingApi.getListingById(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateListing(
        id: String, title: String, description: String, category: String,
        quantity: Int, unit: String, expiryDate: String,
        pickupTimeStart: String, pickupTimeEnd: String, imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.updateListing(id,
                CreateListingRequest(title, description, category, quantity, unit,
                    expiryDate, pickupTimeStart, pickupTimeEnd, imageUrl))
            Result.success(response.data.toDomain())
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun deleteListing(id: String): Result<Unit> {
        return try {
            listingApi.deleteListing(id)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun updateListingQuantity(listingId: String, newQuantity: Int): Result<Listing> {
        return try {
            val response = listingApi.updateListingQuantity(listingId,
                UpdateListingQuantityRequest(newQuantity))
            Result.success(response.data.toDomain())
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun analyzeImage(imageUri: Uri): Result<FoodAnalysisData> {
        return try {
            val file = uriToFile(context, imageUri)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val response = imageAnalysisApi.analyzeImage(multipartBody)
            file.delete()
            if (response.success && response.data != null) Result.success(response.data)
            else Result.failure(Exception(response.message))
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun saveAnalysis(analysisData: FoodAnalysisData): Result<Unit> {
        return try {
            imageAnalysisApi.saveAnalysis(analysisData)
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun uploadFoodImage(imageUri: Uri): Result<String> {
        return try {
            val file = uriToFile(context, imageUri)
            val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val response = imageAnalysisApi.uploadFoodImage(multipartBody)
            file.delete()
            if (response.success && response.imageUrl != null) Result.success(response.imageUrl)
            else Result.failure(Exception(response.message))
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream from URI")
        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
        tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
        inputStream.close()
        return tempFile
    }
}