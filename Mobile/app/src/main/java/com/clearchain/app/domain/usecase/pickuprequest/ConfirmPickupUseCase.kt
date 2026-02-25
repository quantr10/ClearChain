package com.clearchain.app.domain.usecase.pickuprequest

import android.net.Uri
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.repository.PickupRequestRepository
import javax.inject.Inject

class ConfirmPickupUseCase @Inject constructor(
    private val repository: PickupRequestRepository
) {
    // ✅ NEW: With photo
    suspend operator fun invoke(
        requestId: String,
        photoUri: Uri
    ): Result<PickupRequest> {
        return repository.confirmPickupWithPhoto(requestId, photoUri)
    }
}