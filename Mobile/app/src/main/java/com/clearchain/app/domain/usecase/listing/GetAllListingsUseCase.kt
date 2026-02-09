package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class GetAllListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(
        status: String? = null,
        category: String? = null
    ): Result<List<Listing>> {
        return listingRepository.getAllListings(status, category)
    }
}