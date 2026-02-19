package com.clearchain.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.auth.ChangePasswordUseCase
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile -> loadProfile()
            ProfileEvent.ClearError  -> _state.update { it.copy(error = null) }
            is ProfileEvent.ChangePassword -> changePassword(event.currentPassword, event.newPassword)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val user = getCurrentUserUseCase().first()
                _state.update { it.copy(user = user, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(isChangingPassword = true, error = null) }

            val result = changePasswordUseCase(currentPassword, newPassword)

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isChangingPassword = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar("Password changed successfully!"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isChangingPassword = false,
                            error = error.message ?: "Failed to change password"
                        )
                    }
                }
            )
        }
    }
}