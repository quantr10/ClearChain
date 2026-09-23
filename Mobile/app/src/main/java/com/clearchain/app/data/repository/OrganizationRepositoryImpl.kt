package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.remote.api.NgoReputationData
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.dto.UpdateProfileRequest
import com.clearchain.app.domain.repository.OrganizationRepository
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class OrganizationRepositoryImpl @Inject constructor(
    private val api: OrganizationApi,
    private val userDao: UserDao
) : OrganizationRepository {

    override suspend fun getNgoReputation(organizationId: String): Result<NgoReputationData> {
        return try {
            Result.success(api.getNgoReputation(organizationId).data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadVerificationDocument(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        return try {
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("document", fileName, body)
            val url = api.uploadDocument(part).data?.url
                ?: return Result.failure(IllegalStateException("Upload succeeded but no URL returned"))

            // Reflect the new document URL in the local cache
            userDao.getCurrentUser()?.let { current ->
                userDao.insertUser(current.copy(documentUrl = url))
            }
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        return try {
            val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("avatar", fileName, body)
            val url = api.uploadAvatar(part).data?.url
                ?: return Result.failure(IllegalStateException("Upload succeeded but no URL returned"))

            // Reflect the new avatar in the local cache — the dashboards and the
            // profile all read the current user from Room, not from the response.
            userDao.getCurrentUser()?.let { current ->
                userDao.insertUser(current.copy(profilePictureUrl = url))
            }
            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(
        name: String,
        email: String?,
        phone: String?,
        address: String?,
        location: String?,
        state: String?,
        zipCode: String?,
        hours: String?,
        latitude: Double?,
        longitude: Double?,
        contactPerson: String?,
        pickupInstructions: String?,
        description: String?
    ): Result<Unit> {
        return try {
            val request = UpdateProfileRequest(
                name = name, email = email, phone = phone, address = address,
                location = location, state = state, zipCode = zipCode, hours = hours,
                latitude = latitude, longitude = longitude,
                contactPerson = contactPerson,
                pickupInstructions = pickupInstructions,
                description = description
            )
            api.updateProfile(request)

            // local cache
            val currentUser = userDao.getCurrentUser()
            if (currentUser != null) {
                userDao.insertUser(
                    currentUser.copy(
                        name = name,
                        email = email ?: currentUser.email,
                        phone = phone ?: "",
                        address = address ?: "",
                        location = location ?: "",
                        state = state,
                        zipCode = zipCode,
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
