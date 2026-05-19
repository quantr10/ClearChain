package com.clearchain.app.presentation.auth.verify

data class EmailVerificationState(
    val email: String = "",
    val code: String = "",
    val codeError: String? = null,
    val isLoading: Boolean = false,
    val isResending: Boolean = false,
    val resendCooldownSeconds: Int = 0,
    val error: String? = null
)
