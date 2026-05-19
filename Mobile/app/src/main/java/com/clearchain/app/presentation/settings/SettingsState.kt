package com.clearchain.app.presentation.settings

data class SettingsState(
    val theme: String = "system",
    val language: String = "en",
    val notifNewListing: Boolean = true,
    val notifRequestUpdate: Boolean = true,
    val notifExpiry: Boolean = true
)
