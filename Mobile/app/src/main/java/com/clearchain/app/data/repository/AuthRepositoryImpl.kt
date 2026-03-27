package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.toEntity  // CANONICAL from UserEntity.kt
import com.clearchain.app.data.local.entity.toDomain   // CANONICAL from UserEntity.kt
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
        name: String, type: String, email: String,
        password: String, fcmToken: String?
    ): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = RegisterRequest(name = name, type = type, email = email, password = password, fcmToken = fcmToken)
            val response = authApi.register(request)
            val (organization, tokens) = response.data.toDomain()

            userDao.clearUsers()
            authTokenDao.saveTokens(AuthTokenEntity(
                accessToken = tokens.accessToken, refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn, tokenType = tokens.tokenType
            ))
            userDao.insertUser(organization.toEntity())  // uses canonical from UserEntity.kt
            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)
            val (organization, tokens) = response.data.toDomain()

            userDao.clearUsers()
            authTokenDao.saveTokens(AuthTokenEntity(
                accessToken = tokens.accessToken, refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn, tokenType = tokens.tokenType
            ))
            userDao.insertUser(organization.toEntity())
            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            authTokenDao.clearTokens()
            userDao.clearUsers()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = RefreshTokenRequest(refreshToken)
            val response = authApi.refreshToken(request)
            val (organization, tokens) = response.data.toDomain()

            userDao.clearUsers()
            authTokenDao.saveTokens(AuthTokenEntity(
                accessToken = tokens.accessToken, refreshToken = tokens.refreshToken,
                expiresIn = tokens.expiresIn, tokenType = tokens.tokenType
            ))
            userDao.insertUser(organization.toEntity())
            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Flow<Organization?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() }  // uses canonical from UserEntity.kt
    }

    override suspend fun isLoggedIn(): Flow<Boolean> {
        return authTokenDao.getTokensFlow().map { it != null }
    }
}

// NOTE: Private duplicate toEntity()/toDomain() functions REMOVED.
// Now imported from com.clearchain.app.data.local.entity.UserEntity