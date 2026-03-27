package com.clearchain.app.domain.usecase.profile

import com.clearchain.app.domain.repository.OrganizationRepository
import com.clearchain.app.util.ValidationUtils
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(
    private val repository: OrganizationRepository
) {
    suspend operator fun invoke(
        name: String,
        phone: String,
        address: String,
        location: String,
        hours: String? = null,
        // ═══ NEW PARAMS (Part 1) ═══
        latitude: Double? = null,
        longitude: Double? = null,
        contactPerson: String? = null,
        pickupInstructions: String? = null,
        description: String? = null
    ): Result<Unit> {
        if (name.isBlank()) return Result.failure(Exception("Name is required"))
        if (name.length < 3) return Result.failure(Exception("Name must be at least 3 characters"))
        if (phone.isNotBlank() && !ValidationUtils.isValidPhone(phone))
            return Result.failure(Exception("Invalid phone number"))

        return repository.updateProfile(
            name = name,
            phone = phone.ifBlank { null },
            address = address.ifBlank { null },
            location = location.ifBlank { null },
            hours = hours,
            latitude = latitude,
            longitude = longitude,
            contactPerson = contactPerson?.ifBlank { null },
            pickupInstructions = pickupInstructions?.ifBlank { null },
            description = description?.ifBlank { null }
        )
    }
}