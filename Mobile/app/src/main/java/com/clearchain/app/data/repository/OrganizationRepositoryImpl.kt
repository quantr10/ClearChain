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
        hours: String?
    ): Result<Unit> {
        return try {
            val request = UpdateProfileRequest(
                name = name,
                phone = phone,
                address = address,
                location = location,
                hours = hours
            )

            api.updateProfile(request)

            // ✅ FIX: Update local cache using correct method names
            val currentUser = userDao.getCurrentUser()  // ✅ CHANGED: getUser() → getCurrentUser()
            if (currentUser != null) {
                userDao.insertUser(  // ✅ CHANGED: saveUser() → insertUser()
                    currentUser.copy(
                        name = name,
                        phone = phone ?: "",
                        address = address ?: "",
                        location = location ?: "",
                        hours = hours
                    )
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}