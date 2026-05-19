package com.clearchain.app.data.repository

import android.content.Context
import android.net.Uri
import com.clearchain.app.data.local.dao.ListingDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.remote.api.ImageAnalysisApi
import com.clearchain.app.data.remote.api.ListingApi
import com.clearchain.app.data.remote.dto.CreateListingRequest
import com.clearchain.app.data.remote.dto.FoodAnalysisData
import com.clearchain.app.data.remote.dto.UpdateListingQuantityRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class ListingRepositoryImpl @Inject constructor(
    private val listingApi: ListingApi,
    private val imageAnalysisApi: ImageAnalysisApi,
    private val listingDao: ListingDao,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : ListingRepository {

    override suspend fun createListing(
        title: String, description: String, category: String,
        quantity: Int, unit: String, expiryDate: String,
        imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.createListing(
                CreateListingRequest(title, description, category, quantity, unit,
                    expiryDate, imageUrl)
            )
            val domain = response.data.toDomain()
            listingDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun getMyListings(page: Int, pageSize: Int): Result<List<Listing>> {
        return try {
            val response = listingApi.getMyListings(page, pageSize)
            val domain = response.data.map { it.toDomain() }
            listingDao.upsertAll(domain.map { it.toEntity() })
            Result.success(domain)
        } catch (e: Exception) {
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null) {
                val cached = listingDao.observeListingsByGrocery(currentUser.id).first()
                if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
                else Result.failure(e)
            } else Result.failure(e)
        }
    }

    override suspend fun getAllListings(
        status: String?, category: String?,
        lat: Double?, lng: Double?, radiusKm: Int?,
        page: Int, pageSize: Int
    ): Result<List<Listing>> {
        return try {
            val response = listingApi.getAllListings(status = status, category = category, lat = lat, lng = lng, radiusKm = radiusKm, page = page, pageSize = pageSize)
            val domain = response.data.map { it.toDomain() }
            listingDao.upsertAll(domain.map { it.toEntity() })
            Result.success(domain)
        } catch (e: Exception) {
            val cached = listingDao.observeAvailableListings().first()
            if (cached.isNotEmpty()) Result.success(cached.map { it.toDomain() })
            else Result.failure(e)
        }
    }

    override suspend fun getListingById(id: String): Result<Listing> {
        return try {
            val response = listingApi.getListingById(id)
            val domain = response.data.toDomain()
            listingDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            val cached = listingDao.getById(id)
            if (cached != null) Result.success(cached.toDomain())
            else Result.failure(e)
        }
    }

    override suspend fun updateListing(
        id: String, title: String, description: String, category: String,
        quantity: Int, unit: String, expiryDate: String,
        imageUrl: String?
    ): Result<Listing> {
        return try {
            val response = listingApi.updateListing(id,
                CreateListingRequest(title, description, category, quantity, unit,
                    expiryDate, imageUrl))
            val domain = response.data.toDomain()
            listingDao.upsert(domain.toEntity())
            Result.success(domain)
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
            val domain = response.data.toDomain()
            listingDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) { Result.failure(e) }
    }

    override suspend fun analyzeImage(imageUri: Uri): Result<FoodAnalysisData> {
        return try {
            val file = ImageUtils.compressImage(context, imageUri)
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
            val file = ImageUtils.compressImage(context, imageUri)
            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)
            val response = imageAnalysisApi.uploadFoodImage(multipartBody)
            file.delete()
            if (response.success && response.imageUrl != null) Result.success(response.imageUrl)
            else Result.failure(Exception(response.message))
        } catch (e: Exception) { Result.failure(e) }
    }
}
