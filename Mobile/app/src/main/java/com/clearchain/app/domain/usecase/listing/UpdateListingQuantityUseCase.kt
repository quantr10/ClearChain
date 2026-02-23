// domain/usecase/listing/UpdateListingQuantityUseCase.kt - NEW FILE:

package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class UpdateListingQuantityUseCase @Inject constructor(
    private val repository: ListingRepository
) {
    suspend operator fun invoke(
        listingId: String,
        newQuantity: Int
    ): Result<Listing> {
        if (newQuantity <= 0) {
            return Result.failure(
                IllegalArgumentException("Quantity must be greater than 0")
            )
        }
        
        return repository.updateListingQuantity(listingId, newQuantity)
    }
}