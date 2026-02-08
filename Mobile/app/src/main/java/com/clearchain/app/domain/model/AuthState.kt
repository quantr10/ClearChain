package com.clearchain.app.domain.model

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: Organization, val tokens: AuthTokens) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}