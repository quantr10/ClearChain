package com.clearchain.app.domain.repository

import com.clearchain.app.domain.model.PickupRequest

interface PickupRequestRepository {

    suspend fun createPickupRequest(
        listingId: String,
        requestedQuantity: Int,
        pickupDate: String,
        pickupTime: String,
        notes: String? = null
    ): Result<PickupRequest>

    suspend fun getMyPickupRequests(): Result<List<PickupRequest>>

    suspend fun getGroceryPickupRequests(): Result<List<PickupRequest>>

    suspend fun getPickupRequestById(id: String): Result<PickupRequest>

    suspend fun updatePickupRequestStatus(
        id: String,
        status: String
    ): Result<PickupRequest>

    suspend fun cancelPickupRequest(id: String): Result<PickupRequest>
}