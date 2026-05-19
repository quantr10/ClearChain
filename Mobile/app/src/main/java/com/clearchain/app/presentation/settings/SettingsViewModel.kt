package com.clearchain.app.presentation.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsStore.theme,
                settingsStore.language,
                settingsStore.notifNewListing,
                settingsStore.notifRequestUpdate,
                settingsStore.notifExpiry
            ) { values ->
                SettingsState(
                    theme            = values[0] as String,
                    language         = values[1] as String,
                    notifNewListing  = values[2] as Boolean,
                    notifRequestUpdate = values[3] as Boolean,
                    notifExpiry      = values[4] as Boolean
                )
            }.collect { _state.value = it }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                is SettingsEvent.ThemeChanged    -> settingsStore.setTheme(event.theme)
                is SettingsEvent.LanguageChanged -> {
                    settingsStore.setLanguageAndSync(event.language)
                    // Immediately apply — triggers Activity recreation on Android 12 and below;
                    // on Android 13+ the system handles it seamlessly.
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(event.language)
                    )
                }
                is SettingsEvent.NotifNewListingChanged    -> settingsStore.setNotifNewListing(event.enabled)
                is SettingsEvent.NotifRequestUpdateChanged -> settingsStore.setNotifRequestUpdate(event.enabled)
                is SettingsEvent.NotifExpiryChanged        -> settingsStore.setNotifExpiry(event.enabled)
            }
        }
    }
}
