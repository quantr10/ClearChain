package com.clearchain.app.presentation.auth.forgot

sealed class ForgotPasswordEvent {
    data class EmailChanged(val email: String) : ForgotPasswordEvent()
    object SendCode : ForgotPasswordEvent()
    data class CodeChanged(val code: String) : ForgotPasswordEvent()
    data class NewPasswordChanged(val password: String) : ForgotPasswordEvent()
    data class ConfirmPasswordChanged(val password: String) : ForgotPasswordEvent()
    object ResetPassword : ForgotPasswordEvent()
    object ResendCode : ForgotPasswordEvent()
    object ChangeEmail : ForgotPasswordEvent()
}
