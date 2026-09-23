package com.clearchain.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    // Emitted once the new language is on disk. The Activity has to be rebuilt for a locale
    // change to reach already-resolved resources, and only the screen can do that — but it must
    // not do it before the write lands, or attachBaseContext would read the previous value back.
    private val _languageApplied = Channel<Unit>()
    val languageApplied = _languageApplied.receiveAsFlow()

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
                    theme = values[0] as String,
                    language = values[1] as String,
                    notifNewListing = values[2] as Boolean,
                    notifRequestUpdate = values[3] as Boolean,
                    notifExpiry = values[4] as Boolean
                )
            }.collect { _state.value = it }
        }
    }

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            when (event) {
                // Mirrored to SharedPreferences too, so the next cold start paints the chosen
                // palette on its first frame instead of flashing the previous one.
                is SettingsEvent.ThemeChanged -> settingsStore.setThemeAndSync(event.theme)
                is SettingsEvent.LanguageChanged -> {
                    if (event.language != _state.value.language) {
                        settingsStore.setLanguageAndSync(event.language)
                        _languageApplied.send(Unit)
                    }
                }
                is SettingsEvent.NotifNewListingChanged -> settingsStore.setNotifNewListing(event.enabled)
                is SettingsEvent.NotifRequestUpdateChanged -> settingsStore.setNotifRequestUpdate(event.enabled)
                is SettingsEvent.NotifExpiryChanged -> settingsStore.setNotifExpiry(event.enabled)
            }
        }
    }
}
