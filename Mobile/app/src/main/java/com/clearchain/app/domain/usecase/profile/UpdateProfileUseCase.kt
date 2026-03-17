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
        hours: String? = null
    ): Result<Unit> {
        
        // Validate name
        if (name.isBlank()) {
            return Result.failure(Exception("Name is required"))
        }
        
        if (name.length < 3) {
            return Result.failure(Exception("Name must be at least 3 characters"))
        }
        
        // Validate phone (if provided)
        if (phone.isNotBlank() && !ValidationUtils.isValidPhone(phone)) {
            return Result.failure(Exception("Invalid phone number"))
        }
        
        // Call repository
        return repository.updateProfile(
            name = name,
            phone = phone.ifBlank { null },
            address = address.ifBlank { null },
            location = location.ifBlank { null },
            hours = hours
        )
    }
}