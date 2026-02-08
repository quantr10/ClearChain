package com.clearchain.app.util

import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        if (password.length < 8) return false

        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }

        return hasUppercase && hasLowercase && hasDigit
    }

    fun isValidPhone(phone: String): Boolean {
        return phone.isNotBlank() && phone.length >= 10
    }

    fun getPasswordStrength(password: String): PasswordStrength {
        val length = password.length
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        var strength = 0
        if (length >= 8) strength++
        if (length >= 12) strength++
        if (hasUppercase && hasLowercase) strength++
        if (hasDigit) strength++
        if (hasSpecial) strength++

        return when (strength) {
            0, 1 -> PasswordStrength.WEAK
            2, 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }
    }

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}