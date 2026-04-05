package com.clearchain.app.domain.usecase.pickuprequest

import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class GetGroceryPickupRequestsUseCase @Inject constructor(
    private val pickupRequestRepository: PickupRequestRepository
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 20): Result<List<PickupRequest>> {
        return pickupRequestRepository.getGroceryPickupRequests(page, pageSize)
    }
}