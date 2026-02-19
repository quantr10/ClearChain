package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.toEntity
import com.clearchain.app.data.local.entity.toDomain
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
    private val userDao: UserDao,
    private val tokenDao: AuthTokenDao
) : AuthRepository {

    override suspend fun register(
        name: String,
        type: String,
        email: String,
        password: String,
        phone: String,
        address: String,
        location: String,
        hours: String?
    ): Result<Pair<Organization, AuthTokens>> {
        return try {
            val response = authApi.register(
                RegisterRequest(
                    name = name,
                    type = type,
                    email = email,
                    password = password,
                    phone = phone,
                    address = address,
                    location = location,
                    hours = hours
                )
            )

            val (organization, tokens) = response.data.toDomain()

            // Always clear old user before inserting new one
            userDao.clearUsers()
            userDao.insertUser(organization.toEntity())
            tokenDao.saveTokens(
                AuthTokenEntity(
                    accessToken  = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn    = tokens.expiresIn,
                    tokenType    = tokens.tokenType
                )
            )

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
            val response = authApi.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )

            val (organization, tokens) = response.data.toDomain()

            // Always clear old user before inserting new one
            userDao.clearUsers()
            userDao.insertUser(organization.toEntity())
            tokenDao.saveTokens(
                AuthTokenEntity(
                    accessToken  = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    expiresIn    = tokens.expiresIn,
                    tokenType    = tokens.tokenType
                )
            )

            Result.success(Pair(organization, tokens))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val tokens = tokenDao.getTokens()
            if (tokens != null) {
                authApi.logout(RefreshTokenRequest(tokens.refreshToken))
            }
            clearUserData()
            Result.success(Unit)
        } catch (e: Exception) {
            // Even if API call fails, clear local data
            clearUserData()
            Result.success(Unit)
        }
    }

    override suspend fun refreshToken(): Result<AuthTokens> {
        return try {
            val currentTokens = tokenDao.getTokens()
                ?: return Result.failure(Exception("No tokens found"))

            val response = authApi.refreshToken(
                RefreshTokenRequest(currentTokens.refreshToken)
            )

            val newTokens = AuthTokens(
                accessToken  = response.data.accessToken,
                refreshToken = response.data.refreshToken,
                expiresIn    = response.data.expiresIn,
                tokenType    = response.data.tokenType
            )

            tokenDao.saveTokens(
                AuthTokenEntity(
                    accessToken  = newTokens.accessToken,
                    refreshToken = newTokens.refreshToken,
                    expiresIn    = newTokens.expiresIn,
                    tokenType    = newTokens.tokenType
                )
            )

            Result.success(newTokens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Result<Organization> {
        return try {
            val response = authApi.getCurrentUser() // Returns MeResponse
            val (organization, _) = response.data.toDomain() // data is AuthData

            // Always clear old user before updating cache
            userDao.clearUsers()
            userDao.insertUser(organization.toEntity())

            Result.success(organization)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getStoredUser(): Flow<Organization?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() }
    }

    override fun getStoredTokens(): Flow<AuthTokens?> {
        return tokenDao.getTokensFlow().map { tokenEntity ->
            tokenEntity?.let {
                AuthTokens(
                    accessToken  = it.accessToken,
                    refreshToken = it.refreshToken,
                    expiresIn    = it.expiresIn,
                    tokenType    = it.tokenType
                )
            }
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return tokenDao.getTokens() != null
    }

    override suspend fun clearUserData() {
        userDao.clearUsers()
        tokenDao.clearTokens()
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            authApi.changePassword(
                ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword     = newPassword
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}