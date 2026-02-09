package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class CreateListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: String,
        quantity: Int,
        unit: String,
        expiryDate: String,
        pickupTimeStart: String,
        pickupTimeEnd: String,
        imageUrl: String? = null
    ): Result<Listing> {
        // Validate inputs
        if (title.isBlank()) {
            return Result.failure(Exception("Title cannot be empty"))
        }

        if (description.isBlank()) {
            return Result.failure(Exception("Description cannot be empty"))
        }

        if (quantity <= 0) {
            return Result.failure(Exception("Quantity must be greater than 0"))
        }

        if (unit.isBlank()) {
            return Result.failure(Exception("Unit cannot be empty"))
        }

        return listingRepository.createListing(
            title = title.trim(),
            description = description.trim(),
            category = category,
            quantity = quantity,
            unit = unit.trim(),
            expiryDate = expiryDate,
            pickupTimeStart = pickupTimeStart,
            pickupTimeEnd = pickupTimeEnd,
            imageUrl = imageUrl
        )
    }
}