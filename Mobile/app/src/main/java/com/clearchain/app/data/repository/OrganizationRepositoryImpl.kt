package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.UpdateProfileRequest
import com.clearchain.app.domain.repository.OrganizationRepository
import javax.inject.Inject

class OrganizationRepositoryImpl @Inject constructor(
    private val api: OrganizationApi,
    private val userDao: UserDao
) : OrganizationRepository {

    override suspend fun updateProfile(
        name: String,
        phone: String?,
        address: String?,
        location: String?,
        hours: String?,
        latitude: Double?,
        longitude: Double?,
        contactPerson: String?,
        pickupInstructions: String?,
        description: String?
    ): Result<Unit> {
        return try {
            val request = UpdateProfileRequest(
                name = name, phone = phone, address = address,
                location = location, hours = hours,
                latitude = latitude, longitude = longitude,
                contactPerson = contactPerson,
                pickupInstructions = pickupInstructions,
                description = description
            )
            api.updateProfile(request)

            // Update local cache
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null) {
                userDao.insertUser(
                    currentUser.copy(
                        name = name,
                        phone = phone ?: "",
                        address = address ?: "",
                        location = location ?: "",
                        hours = hours,
                        latitude = latitude,
                        longitude = longitude,
                        contactPerson = contactPerson,
                        pickupInstructions = pickupInstructions,
                        description = description
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}