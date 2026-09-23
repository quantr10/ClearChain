package com.clearchain.app.domain.usecase.pickuprequest

import com.clearchain.app.domain.repository.BulkActionOutcome
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class BulkApprovePickupRequestsUseCase @Inject constructor(
    private val pickupRequestRepository: PickupRequestRepository
) {
    suspend operator fun invoke(ids: List<String>): Result<BulkActionOutcome> =
        pickupRequestRepository.bulkApprovePickupRequests(ids)
}
