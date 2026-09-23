package com.clearchain.app.data.repository

import com.clearchain.app.data.local.dao.AuthTokenDao
import com.clearchain.app.data.local.dao.NotificationDao
import com.clearchain.app.data.local.dao.UserDao
import com.clearchain.app.data.local.database.ClearChainDatabase
import com.clearchain.app.data.local.entity.AuthTokenEntity
import com.clearchain.app.data.local.entity.toDomain // CANONICAL from UserEntity.kt
import com.clearchain.app.data.local.entity.toEntity // CANONICAL from UserEntity.kt
import com.clearchain.app.data.remote.api.AuthApi
import com.clearchain.app.data.remote.dto.*
import com.clearchain.app.domain.model.AuthTokens
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val authTokenDao: AuthTokenDao,
    private val userDao: UserDao,
    private val notificationDao: NotificationDao,
    private val database: ClearChainDatabase
) : AuthRepository {

    override suspend fun register(
        name: String,
        type: String,
        email: String,
        password: String,
        fcmToken: String?
    ): Result<String> {
        return try {
            val request = RegisterRequest(name = name, type = type, email = email, password = password, fcmToken = fcmToken)
            authApi.register(request)
            Result.success(email)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyEmail(email: String, code: String): Result<Pair<Organization, AuthTokens>> {
        return try {
            val response = authApi.verifyEmail(VerifyEmailRequest(email, code))
            val (organization, tokens) = response.data.toDomain()

            clearPreviousAccountCache()
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

    override suspend fun resendVerification(email: String): Result<Unit> {
        return try {
            authApi.resendVerification(ResendVerificationRequest(email))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Pair<Organization, AuthTokens>> {
        return try {
            val request = LoginRequest(email, password)
            val response = authApi.login(request)
            val (organization, tokens) = response.data.toDomain()

            clearPreviousAccountCache()
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

    /**
     * Drops cached rows that belong to whoever was signed in before.
     *
     * Called from the two paths that hand this device a session for a *different* account —
     * login and email verification — and deliberately not from [refreshToken], which renews the
     * same one.
     *
     * The notification cache has no account column: it is one table shared by every account
     * that has ever signed in here, and sync only upserts, so a row left behind by a previous
     * account is never removed and shows up in the next user's inbox. Logout already clears it,
     * but a session that simply expires never reaches logout — it drops the user on the Login
     * screen instead, which is why the clearing belongs here rather than only there.
     *
     * Local only: [NotificationDao.clearAll] is a delete against Room, not the inbox endpoint,
     * so the previous account's notifications stay untouched on the server.
     */
    private suspend fun clearPreviousAccountCache() {
        notificationDao.clearAll()
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            // Revoke refresh token on server before clearing local state
            val tokens = authTokenDao.getTokens()
            if (tokens?.refreshToken != null) {
                try {
                    // Sending this device's FCM token lets the server unregister it —
                    // without it, this device kept receiving the departing account's
                    // push notifications until the stale-token sweep or another login.
                    val fcmToken = database.fcmTokenDao().getToken()
                    authApi.logout(LogoutRequest(tokens.refreshToken, fcmToken))
                } catch (_: Exception) {
                    // Non-fatal: local state is still cleared even if server call fails
                }
            }
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

    override suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            authApi.changePassword(ChangePasswordRequest(currentPassword, newPassword))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): Flow<Organization?> {
        return userDao.getCurrentUserFlow().map { it?.toDomain() } // uses canonical from UserEntity.kt
    }

    override suspend fun refreshCurrentUser(): Result<Organization> {
        return try {
            val organization = authApi.getCurrentUser().data.user.toDomain()
            userDao.insertUser(organization.toEntity()) // REPLACE on conflict — keeps same row
            Result.success(organization)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isLoggedIn(): Flow<Boolean> {
        return authTokenDao.getTokensFlow().map { it != null }
    }
}

// NOTE: Private duplicate toEntity()/toDomain() functions REMOVED.
// Now imported from com.clearchain.app.data.local.entity.UserEntity
