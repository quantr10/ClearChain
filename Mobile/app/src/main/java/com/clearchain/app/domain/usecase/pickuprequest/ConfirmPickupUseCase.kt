package com.clearchain.app.domain.usecase.pickuprequest

import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class ConfirmPickupUseCase @Inject constructor(
    private val repository: PickupRequestRepository
) {
    suspend operator fun invoke(requestId: String): Result<PickupRequest> {
        return repository.markPickedUp(requestId)
    }
}