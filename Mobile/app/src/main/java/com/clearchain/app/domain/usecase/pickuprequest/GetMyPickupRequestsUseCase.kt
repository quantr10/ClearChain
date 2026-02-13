package com.clearchain.app.domain.usecase.pickuprequest

import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class GetMyPickupRequestsUseCase @Inject constructor(
    private val pickupRequestRepository: PickupRequestRepository
) {
    suspend operator fun invoke(): Result<List<PickupRequest>> {
        return pickupRequestRepository.getMyPickupRequests()
    }
}