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

    /**
     * Shared by onboarding and profile-edit (previously two different rules — onboarding
     * required a leading "+" and validated digit count, profile-edit only checked overall
     * string length — so a number valid from one screen could be rejected re-entering
     * the other). An optional leading "+", 10-15 digits, with common separators allowed.
     */
    fun isValidPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        val digits = trimmed.removePrefix("+").filter(Char::isDigit)
        return digits.length in 10..15 &&
            trimmed.all { it.isDigit() || it in setOf('+', ' ', '-', '(', ')') }
    }
}
