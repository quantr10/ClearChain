package com.clearchain.app.presentation.settings

import com.clearchain.app.data.local.SettingsStore

data class SettingsState(
    val theme: String = SettingsStore.THEME_SYSTEM,
    val language: String = SettingsStore.LANG_EN,
    val notifNewListing: Boolean = true,
    val notifRequestUpdate: Boolean = true,
    val notifExpiry: Boolean = true
)
