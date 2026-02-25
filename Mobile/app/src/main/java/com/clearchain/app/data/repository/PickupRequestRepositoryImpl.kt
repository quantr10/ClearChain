package com.clearchain.app.data.repository

import android.content.Context
import android.net.Uri
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.CreatePickupRequestRequest
import com.clearchain.app.data.remote.dto.UpdatePickupRequestStatusRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class PickupRequestRepositoryImpl @Inject constructor(
    private val pickupRequestApi: PickupRequestApi,
    @ApplicationContext private val context: Context  // ✅ Add context
) : PickupRequestRepository {

    override suspend fun createPickupRequest(
        listingId: String,
        requestedQuantity: Int,
        pickupDate: String,
        pickupTime: String,
        notes: String?
    ): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.createPickupRequest(
                CreatePickupRequestRequest(
                    listingId = listingId,
                    requestedQuantity = requestedQuantity,
                    pickupDate = pickupDate,
                    pickupTime = pickupTime,
                    notes = notes
                )
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyPickupRequests(): Result<List<PickupRequest>> {
        return try {
            val response = pickupRequestApi.getMyPickupRequests()
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGroceryPickupRequests(): Result<List<PickupRequest>> {
        return try {
            val response = pickupRequestApi.getGroceryPickupRequests()
            Result.success(response.data.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPickupRequestById(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.getPickupRequestById(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePickupRequestStatus(
        id: String,
        status: String
    ): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.updatePickupRequestStatus(
                id,
                UpdatePickupRequestStatusRequest(status)
            )
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cancelPickupRequest(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.cancelPickupRequest(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approvePickupRequest(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.approvePickupRequest(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun rejectPickupRequest(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.rejectPickupRequest(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markReadyForPickup(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.markReadyForPickup(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ NEW METHOD (with photo upload)
    override suspend fun confirmPickupWithPhoto(
        id: String,
        photoUri: Uri
    ): Result<PickupRequest> {
        return try {
            // Convert URI to File
            val file = uriToFile(photoUri)
            
            // Create multipart body
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val photoPart = MultipartBody.Part.createFormData(
                "proofPhoto",  // ✅ Must match backend parameter name
                file.name,
                requestFile
            )
            
            // Make API call
            val response = pickupRequestApi.confirmPickupWithPhoto(id, photoPart)
            
            // Clean up temp file
            file.delete()
            
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ Helper: Convert URI to File
    private fun uriToFile(uri: Uri): File {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile(
            "upload_${System.currentTimeMillis()}", 
            ".jpg", 
            context.cacheDir
        )
        
        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
}