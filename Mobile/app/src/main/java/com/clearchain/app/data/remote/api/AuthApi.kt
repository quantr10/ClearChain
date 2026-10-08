package com.clearchain.app.data.remote.api

import com.clearchain.app.data.remote.dto.*
import retrofit2.http.*

interface AuthApi {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): MessageResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): AuthResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(
        @Body request: ResendVerificationRequest
    ): MessageResponse

    @POST("auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): MessageResponse

    @POST("auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): MessageResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest
    ): Unit

    @GET("auth/me")
    suspend fun getCurrentUser(): MeResponse

    @POST("auth/change-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): MessageResponse

    @POST("auth/fcm-token")
    suspend fun registerFCMToken(@Body request: RegisterFCMTokenRequest): ApiResponse<Unit>

    @GET("auth/check-email")
    suspend fun checkEmail(@Query("email") email: String): EmailAvailabilityResponse

    @DELETE("auth/account")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): MessageResponse
}
