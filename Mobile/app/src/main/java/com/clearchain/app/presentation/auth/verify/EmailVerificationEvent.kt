package com.clearchain.app.presentation.auth.verify

sealed class EmailVerificationEvent {
    data class CodeChanged(val code: String) : EmailVerificationEvent()
    object Verify : EmailVerificationEvent()
    object ResendCode : EmailVerificationEvent()
    object ClearError : EmailVerificationEvent()
}
