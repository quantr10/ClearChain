package com.clearchain.app.presentation.auth.forgot

data class ForgotPasswordState(
    val email: String = "",
    val emailError: String? = null,
    /** False while asking for the email, true once a code has been sent to it. */
    val codeSent: Boolean = false,
    val code: String = "",
    val codeError: String? = null,
    val newPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isSending: Boolean = false,
    val isResetting: Boolean = false,
    val resendCooldownSeconds: Int = 0
) {
    val isBusy: Boolean get() = isSending || isResetting
}
