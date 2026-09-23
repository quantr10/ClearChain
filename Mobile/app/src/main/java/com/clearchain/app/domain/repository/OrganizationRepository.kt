package com.clearchain.app.domain.repository

import com.clearchain.app.data.remote.api.NgoReputationData

interface OrganizationRepository {

    /**
     * Another org's reputation stats. No local cache target exists for this — it's read-only
     * information about a different organization, not the signed-in user's own data — so this
     * is a straight passthrough to the API, added only for architectural consistency (every
     * other organization action goes through this repository).
     */
    suspend fun getNgoReputation(organizationId: String): Result<NgoReputationData>

    /**
     * Uploads the single verification document (business licence / charity certificate).
     * Re-uploading replaces the existing document.
     * @return the stored document URL on success.
     */
    suspend fun uploadVerificationDocument(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String>

    /**
     * Uploads a new profile picture and writes the returned URL into the cached
     * user, so every screen reading the current user reflects it immediately.
     * @return the stored avatar URL on success.
     */
    suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String>

    suspend fun updateProfile(
        name: String,
        email: String?,
        phone: String?,
        address: String?,
        location: String?,
        state: String?,
        zipCode: String?,
        hours: String?,
        latitude: Double? = null,
        longitude: Double? = null,
        contactPerson: String? = null,
        pickupInstructions: String? = null,
        description: String? = null
    ): Result<Unit>
}
