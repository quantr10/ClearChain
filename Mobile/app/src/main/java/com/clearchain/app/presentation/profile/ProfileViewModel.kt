package com.clearchain.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.auth.ChangePasswordUseCase
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.profile.UpdateProfileUseCase
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.ValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase  // ✅ NEW
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
            
            // ✅ NEW: Edit mode events
            ProfileEvent.StartEdit -> startEdit()
            ProfileEvent.CancelEdit -> cancelEdit()
            is ProfileEvent.EditNameChanged -> {
                _state.update { it.copy(editName = event.name, editNameError = null) }
            }
            is ProfileEvent.EditPhoneChanged -> {
                _state.update { it.copy(editPhone = event.phone, editPhoneError = null) }
            }
            is ProfileEvent.EditAddressChanged -> {
                _state.update { it.copy(editAddress = event.address, editAddressError = null) }
            }
            is ProfileEvent.EditLocationChanged -> {
                _state.update { it.copy(editLocation = event.location, editLocationError = null) }
            }
            is ProfileEvent.EditHoursChanged -> {
                _state.update { it.copy(editHours = event.hours) }
            }
            ProfileEvent.SaveProfile -> saveProfile()
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

    // ✅ NEW: Start editing - populate fields with current values
    private fun startEdit() {
        val user = _state.value.user ?: return
        
        _state.update {
            it.copy(
                isEditing = true,
                editName = user.name,
                editPhone = user.phone,
                editAddress = user.address,
                editLocation = user.location,
                editHours = user.hours ?: "",
                // Clear errors
                editNameError = null,
                editPhoneError = null,
                editAddressError = null,
                editLocationError = null,
                error = null
            )
        }
    }

    // ✅ NEW: Cancel editing - revert to original values
    private fun cancelEdit() {
        _state.update {
            it.copy(
                isEditing = false,
                editNameError = null,
                editPhoneError = null,
                editAddressError = null,
                editLocationError = null,
                error = null
            )
        }
    }

    // ✅ NEW: Save profile changes
    private fun saveProfile() {
        val currentState = _state.value
        
        // Validate inputs
        if (!validateProfileInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true, error = null) }

            val result = updateProfileUseCase(
                name = currentState.editName,
                phone = currentState.editPhone,
                address = currentState.editAddress,
                location = currentState.editLocation,
                hours = currentState.editHours.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    // Reload profile to get updated data
                    loadProfile()
                    
                    _state.update { 
                        it.copy(
                            isSavingProfile = false,
                            isEditing = false
                        ) 
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar("Profile updated successfully!"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isSavingProfile = false,
                            error = error.message ?: "Failed to update profile"
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Failed to update profile"))
                }
            )
        }
    }

    // ✅ NEW: Validate profile inputs
    private fun validateProfileInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        // Validate name
        if (currentState.editName.isBlank()) {
            _state.update { it.copy(editNameError = "Name is required") }
            isValid = false
        } else if (currentState.editName.length < 3) {
            _state.update { it.copy(editNameError = "Name must be at least 3 characters") }
            isValid = false
        }

        // Validate phone (if provided)
        if (currentState.editPhone.isNotBlank() && !ValidationUtils.isValidPhone(currentState.editPhone)) {
            _state.update { it.copy(editPhoneError = "Invalid phone number") }
            isValid = false
        }

        // Address and Location are optional, no validation needed

        return isValid
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