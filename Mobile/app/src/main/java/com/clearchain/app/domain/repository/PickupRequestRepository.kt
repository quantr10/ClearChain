package com.clearchain.app.domain.repository

import android.net.Uri
import com.clearchain.app.domain.model.PickupRequest

interface PickupRequestRepository {

    suspend fun createPickupRequest(
        listingId: String,
        requestedQuantity: Int,
        pickupDate: String,
        pickupTime: String,
        notes: String? = null,
        requiresRefrigeration: Boolean = false,
        isFragile: Boolean = false,
        isHeavy: Boolean = false
    ): Result<PickupRequest>

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

    suspend fun bulkApprovePickupRequests(ids: List<String>): Result<BulkActionOutcome>

    suspend fun bulkRejectPickupRequests(ids: List<String>, reason: String?): Result<BulkActionOutcome>
}

/** How many of a bulk approve/reject actually went through. */
data class BulkActionOutcome(val succeeded: Int, val failed: Int)
