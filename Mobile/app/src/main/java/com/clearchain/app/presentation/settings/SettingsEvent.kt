package com.clearchain.app.presentation.settings

sealed class SettingsEvent {
    data class ThemeChanged(val theme: String) : SettingsEvent()
    data class LanguageChanged(val language: String) : SettingsEvent()
    data class NotifNewListingChanged(val enabled: Boolean) : SettingsEvent()
    data class NotifRequestUpdateChanged(val enabled: Boolean) : SettingsEvent()
    data class NotifExpiryChanged(val enabled: Boolean) : SettingsEvent()
}
