package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import javax.inject.Inject

class GetAllListingsUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    // ═══ UPDATED: Added location params (Part 2) ═══
    suspend operator fun invoke(
        status: String? = null,
        category: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Int? = null,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<List<Listing>> {
        return listingRepository.getAllListings(status, category, lat, lng, radiusKm, page, pageSize)
    }
}