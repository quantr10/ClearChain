package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class GetMyListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(): Result<List<Listing>> {
        return listingRepository.getMyListings()
    }
}