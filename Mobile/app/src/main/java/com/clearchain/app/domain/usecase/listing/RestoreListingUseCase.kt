package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class RestoreListingUseCase @Inject constructor(
    private val repository: ListingRepository
) {
    suspend operator fun invoke(listingId: String): Result<Listing> =
        repository.restoreListing(listingId)
}
