package com.clearchain.app.domain.model

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val tokenType: String = "Bearer"
)