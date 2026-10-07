package com.clearchain.app.domain.repository

import android.net.Uri
import com.clearchain.app.domain.model.PickupRequest

interface PickupRequestRepository {

    suspend fun getMyPickupRequests(page: Int = 1, pageSize: Int = 20): Result<List<PickupRequest>>

    suspend fun getGroceryPickupRequests(page: Int = 1, pageSize: Int = 20): Result<List<PickupRequest>>

    suspend fun getPickupRequestById(id: String): Result<PickupRequest>

    suspend fun cancelPickupRequest(id: String): Result<PickupRequest>

    suspend fun approvePickupRequest(id: String): Result<PickupRequest>

    suspend fun markReadyForPickup(id: String): Result<PickupRequest>

    // ✅ NEW METHOD (with photo)
    suspend fun confirmPickupWithPhoto(
        id: String,
        photoUri: Uri
    ): Result<PickupRequest>
}
