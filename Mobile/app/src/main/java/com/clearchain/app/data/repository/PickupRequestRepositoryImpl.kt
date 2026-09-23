package com.clearchain.app.data.repository

import android.content.Context
import android.net.Uri
import com.clearchain.app.data.local.dao.PickupRequestDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.toDomain
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.BulkActionRequest
import com.clearchain.app.data.remote.dto.BulkRejectRequest
import com.clearchain.app.data.remote.dto.CreatePickupRequestRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.BulkActionOutcome
import com.clearchain.app.domain.repository.PickupRequestRepository
import com.clearchain.app.util.ImageUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class PickupRequestRepositoryImpl @Inject constructor(
    private val pickupRequestApi: PickupRequestApi,
    private val pickupRequestDao: PickupRequestDao,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : PickupRequestRepository {

    override suspend fun createPickupRequest(
        listingId: String,
        requestedQuantity: Int,
        pickupDate: String,
        pickupTime: String,
        notes: String?,
        requiresRefrigeration: Boolean,
        isFragile: Boolean,
        isHeavy: Boolean
    ): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.createPickupRequest(
                CreatePickupRequestRequest(
                    listingId = listingId,
                    requestedQuantity = requestedQuantity,
                    pickupDate = pickupDate,
                    pickupTime = pickupTime,
                    notes = notes,
                    requiresRefrigeration = requiresRefrigeration,
                    isFragile = isFragile,
                    isHeavy = isHeavy
                )
            )
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMyPickupRequests(page: Int, pageSize: Int): Result<List<PickupRequest>> {
        return try {
            val response = pickupRequestApi.getMyPickupRequests(page, pageSize)
            val domain = response.data.map { it.toDomain() }
            pickupRequestDao.upsertAll(domain.map { it.toEntity() })
            Result.success(domain)
        } catch (e: Exception) {
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null) {
                val cached = pickupRequestDao.observeByNgo(currentUser.id).first()
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDomain() })
                } else {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getGroceryPickupRequests(page: Int, pageSize: Int): Result<List<PickupRequest>> {
        return try {
            val response = pickupRequestApi.getGroceryPickupRequests(page, pageSize)
            val domain = response.data.map { it.toDomain() }
            pickupRequestDao.upsertAll(domain.map { it.toEntity() })
            Result.success(domain)
        } catch (e: Exception) {
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null) {
                val cached = pickupRequestDao.observeByGrocery(currentUser.id).first()
                if (cached.isNotEmpty()) {
                    Result.success(cached.map { it.toDomain() })
                } else {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPickupRequestById(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.getPickupRequestById(id)
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            val cached = pickupRequestDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun cancelPickupRequest(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.cancelPickupRequest(id)
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approvePickupRequest(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.approvePickupRequest(id)
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markReadyForPickup(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.markReadyForPickup(id)
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun confirmPickupWithPhoto(
        id: String,
        photoUri: Uri
    ): Result<PickupRequest> {
        return try {
            val file = ImageUtils.compressImage(context, photoUri)
            val response = try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("proofPhoto", file.name, requestFile)
                pickupRequestApi.confirmPickupWithPhoto(id, photoPart)
            } finally {
                // Must run even if the call throws, or a failed confirmation leaks this file.
                file.delete()
            }
            val domain = response.data.toDomain()
            pickupRequestDao.upsert(domain.toEntity())
            Result.success(domain)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkApprovePickupRequests(ids: List<String>): Result<BulkActionOutcome> {
        return try {
            val response = pickupRequestApi.bulkApprove(BulkActionRequest(ids))
            patchCachedStatus(response.results, newStatus = "approved")
            val succeeded = response.results.count { it.success }
            Result.success(BulkActionOutcome(succeeded = succeeded, failed = response.results.size - succeeded))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun bulkRejectPickupRequests(ids: List<String>, reason: String?): Result<BulkActionOutcome> {
        return try {
            val response = pickupRequestApi.bulkReject(BulkRejectRequest(ids, reason))
            patchCachedStatus(response.results, newStatus = "rejected")
            val succeeded = response.results.count { it.success }
            Result.success(BulkActionOutcome(succeeded = succeeded, failed = response.results.size - succeeded))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * The bulk endpoints return per-id success flags, not the updated requests themselves, so
     * each succeeded id's cached copy is patched with the new status directly rather than
     * re-fetched — avoids N extra network calls just to keep the offline cache in sync.
     */
    private suspend fun patchCachedStatus(
        results: List<com.clearchain.app.data.remote.dto.BulkActionResultItem>,
        newStatus: String
    ) {
        results.filter { it.success }.forEach { result ->
            pickupRequestDao.getById(result.id)?.let { cached ->
                pickupRequestDao.upsert(cached.copy(status = newStatus))
            }
        }
    }
}
