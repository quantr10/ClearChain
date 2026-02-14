package com.clearchain.app.data.repository

import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.CreatePickupRequestRequest
import com.clearchain.app.data.remote.dto.UpdatePickupRequestStatusRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class PickupRequestRepositoryImpl @Inject constructor(
    private val pickupRequestApi: PickupRequestApi
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

    override suspend fun markPickedUp(id: String): Result<PickupRequest> {
        return try {
            val response = pickupRequestApi.markPickedUp(id)
            Result.success(response.data.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}