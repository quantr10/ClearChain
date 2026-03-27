package com.clearchain.app.domain.repository

interface OrganizationRepository {
    suspend fun updateProfile(
        name: String,
        phone: String?,
        address: String?,
        location: String?,
        hours: String?,
        // ═══ NEW PARAMS (Part 1) ═══
        latitude: Double? = null,
        longitude: Double? = null,
        contactPerson: String? = null,
        pickupInstructions: String? = null,
        description: String? = null
    ): Result<Unit>
}