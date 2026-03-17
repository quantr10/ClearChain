package com.clearchain.app.domain.repository

interface OrganizationRepository {
    suspend fun updateProfile(
        name: String,
        phone: String?,
        address: String?,
        location: String?,
        hours: String?
    ): Result<Unit>
}