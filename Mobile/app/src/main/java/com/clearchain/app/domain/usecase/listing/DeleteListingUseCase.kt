package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class DeleteListingUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return listingRepository.deleteListing(id)
    }
}