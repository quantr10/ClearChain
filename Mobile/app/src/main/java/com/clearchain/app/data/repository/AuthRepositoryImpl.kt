package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.UserEntity
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.ChangePasswordRequest
import com.clearchain.app.data.remote.dto.LoginRequest
import com.clearchain.app.data.remote.dto.RefreshTokenRequest
import com.clearchain.app.data.remote.dto.RegisterRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val authTokenDao: AuthTokenDao,
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun register(
    name: String,
    type: String,
    email: String,
    password: String,
    fcmToken: String?
): Result<Pair<Organization, AuthTokens>> {
    return try {
        val request = RegisterRequest(
            name = name,
            type = type,
            email = email,
            password = password,
            fcmToken = fcmToken
        )

        val response = authApi.register(request)
        val (organization, tokens) = response.data.toDomain()

        // ✅ ADD: Clear old users BEFORE saving new one
        userDao.clearUsers()
        
        authTokenDao.saveTokens(
            AuthTokenEntity(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn,
                tokenType = tokens.tokenType
            )
        )

        userDao.insertUser(organization.toEntity())

        Result.success(Pair(organization, tokens))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

    override suspend fun login(
        email: String,
        password: String
    ): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)
            val (organization, tokens) = response.data.toDomain()

            // ✅ ADD: Clear old users BEFORE saving new one
            userDao.clearUsers()
            
            authTokenDao.saveTokens(
                AuthTokenEntity(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = tokens.expiresIn,
                    tokenType = tokens.tokenType
                )
            )

            userDao.insertUser(organization.toEntity())

            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            // Clear local data
            authTokenDao.clearTokens()
            userDao.clearUsers()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(
        refreshToken: String
    ): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = RefreshTokenRequest(refreshToken)
            val response = authApi.refreshToken(request)
            val (organization, tokens) = response.data.toDomain()

            // ✅ ADD: Clear old users BEFORE saving new one
            userDao.clearUsers()
            
            authTokenDao.saveTokens(
                AuthTokenEntity(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn = tokens.expiresIn,
                    tokenType = tokens.tokenType
                )
            )

            userDao.insertUser(organization.toEntity())

            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            authApi.changePassword(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ✅ FIX: Return Flow directly (not suspend fun)
    override suspend fun getCurrentUser(): Flow<Organization?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() }
    }

    // ✅ FIX: Return Flow directly (not suspend fun)
    override suspend fun isLoggedIn(): Flow<Boolean> {
        return authTokenDao.getTokensFlow().map { it != null }
    }
}

// ✅ Extension functions for mapping
private fun Organization.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        type = type.name.lowercase(),
        phone = phone,
        address = address,
        location = location,
        verified = verified,
        verificationStatus = verificationStatus.name.lowercase(),
        hours = hours,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt
    )
}

private fun UserEntity.toDomain(): Organization {
    return Organization(
        id = id,
        name = name,
        type = when (type.lowercase()) {
            "grocery" -> com.clearchain.app.domain.model.OrganizationType.GROCERY
            "ngo" -> com.clearchain.app.domain.model.OrganizationType.NGO
            "admin" -> com.clearchain.app.domain.model.OrganizationType.ADMIN
            else -> com.clearchain.app.domain.model.OrganizationType.GROCERY
        },
        email = email,
        phone = phone,
        address = address,
        location = location,
        verified = verified,
        verificationStatus = when (verificationStatus.lowercase()) {
            "approved" -> com.clearchain.app.domain.model.VerificationStatus.APPROVED
            "rejected" -> com.clearchain.app.domain.model.VerificationStatus.REJECTED
            else -> com.clearchain.app.domain.model.VerificationStatus.PENDING
        },
        hours = hours,
        profilePictureUrl = profilePictureUrl,
        createdAt = createdAt
    )
}