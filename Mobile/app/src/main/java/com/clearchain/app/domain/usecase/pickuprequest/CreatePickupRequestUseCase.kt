package com.clearchain.app.domain.usecase.pickuprequest

import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class CreatePickupRequestUseCase @Inject constructor(
    private val pickupRequestRepository: PickupRequestRepository
) {
    suspend operator fun invoke(
        listingId: String,
        requestedQuantity: Int,
        pickupDate: String,
        pickupTime: String,
        notes: String? = null,
        vehicleType: String? = null,
        requiresRefrigeration: Boolean = false,
        isFragile: Boolean = false,
        isHeavy: Boolean = false
    ): Result<PickupRequest> {
        if (requestedQuantity <= 0) {
            return Result.failure(Exception("Quantity must be greater than 0"))
        }
        if (pickupDate.isBlank()) {
            return Result.failure(Exception("Pickup date is required"))
        }
        if (pickupTime.isBlank()) {
            return Result.failure(Exception("Pickup time is required"))
        }

        return pickupRequestRepository.createPickupRequest(
            listingId = listingId,
            requestedQuantity = requestedQuantity,
            pickupDate = pickupDate,
            pickupTime = pickupTime,
            notes = notes,
            vehicleType = vehicleType,
            requiresRefrigeration = requiresRefrigeration,
            isFragile = isFragile,
            isHeavy = isHeavy
        )
    }
}