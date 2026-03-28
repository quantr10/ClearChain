package com.clearchain.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.model.OrganizationType
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
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init { loadProfile() }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile -> loadProfile()
            ProfileEvent.ClearError -> _state.update { it.copy(error = null) }
            is ProfileEvent.ChangePassword -> changePassword(event.currentPassword, event.newPassword)
            ProfileEvent.StartEdit -> startEdit()
            ProfileEvent.CancelEdit -> cancelEdit()
            is ProfileEvent.EditNameChanged ->
                _state.update { it.copy(editName = event.name, editNameError = null) }
            is ProfileEvent.EditPhoneChanged ->
                _state.update { it.copy(editPhone = event.phone, editPhoneError = null) }
            is ProfileEvent.EditAddressChanged ->
                _state.update { it.copy(editAddress = event.address, editAddressError = null) }
            is ProfileEvent.EditLocationChanged ->
                _state.update { it.copy(editLocation = event.location, editLocationError = null) }
            is ProfileEvent.EditOpenTimeChanged ->
                _state.update { it.copy(editOpenTime = event.time) }
            is ProfileEvent.EditCloseTimeChanged ->
                _state.update { it.copy(editCloseTime = event.time) }
            // ═══ NEW event handlers (Part 1) ═══
            is ProfileEvent.EditContactPersonChanged ->
                _state.update { it.copy(editContactPerson = event.contactPerson, editContactPersonError = null) }
            is ProfileEvent.EditPickupInstructionsChanged ->
                _state.update { it.copy(editPickupInstructions = event.instructions) }
            is ProfileEvent.EditDescriptionChanged ->
                _state.update { it.copy(editDescription = event.description) }
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

    // ═══ UPDATED: populates new fields (Part 1) ═══
    private fun startEdit() {
        val user = _state.value.user ?: return
        _state.update {
            it.copy(
                isEditing = true,
                editName = user.name, editPhone = user.phone,
                editAddress = user.address, editLocation = user.location,
                editOpenTime = user.hours?.substringBefore(" - ", "") ?: "",
                editCloseTime = user.hours?.substringAfter(" - ", "") ?: "",
                editContactPerson = user.contactPerson ?: "",
                editPickupInstructions = user.pickupInstructions ?: "",
                editDescription = user.description ?: "",
                editNameError = null, editPhoneError = null,
                editContactPersonError = null, error = null
            )
        }
    }

    private fun cancelEdit() {
        _state.update {
            it.copy(isEditing = false, editNameError = null,
                editPhoneError = null, editContactPersonError = null, error = null)
        }
    }

    // ═══ UPDATED: passes new fields (Part 1) ═══
    private fun saveProfile() {
        if (!validateProfileInputs()) return
        val s = _state.value

        viewModelScope.launch {
            _state.update { it.copy(isSavingProfile = true, error = null) }

            val result = updateProfileUseCase(
                name = s.editName, phone = s.editPhone,
                address = s.editAddress, location = s.editLocation,
                hours = if (s.editOpenTime.isNotBlank() && s.editCloseTime.isNotBlank())
                    "${s.editOpenTime} - ${s.editCloseTime}" else null,
                latitude = null, longitude = null,
                contactPerson = s.editContactPerson.ifBlank { null },
                pickupInstructions = s.editPickupInstructions.ifBlank { null },
                description = s.editDescription.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    loadProfile()
                    _state.update { it.copy(isSavingProfile = false, isEditing = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar("Profile updated successfully!"))
                },
                onFailure = { error ->
                    _state.update { it.copy(isSavingProfile = false, error = error.message ?: "Failed to update profile") }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Failed to update profile"))
                }
            )
        }
    }

    // ═══ UPDATED: validates NGO contact person (Part 1) ═══
    private fun validateProfileInputs(): Boolean {
        val s = _state.value
        var valid = true
        if (s.editName.isBlank()) {
            _state.update { it.copy(editNameError = "Name is required") }; valid = false
        } else if (s.editName.length < 3) {
            _state.update { it.copy(editNameError = "Name must be at least 3 characters") }; valid = false
        }
        if (s.editPhone.isNotBlank() && !ValidationUtils.isValidPhone(s.editPhone)) {
            _state.update { it.copy(editPhoneError = "Invalid phone number") }; valid = false
        }
        if ((s.user?.type == OrganizationType.NGO || s.user?.type == OrganizationType.GROCERY) && s.editContactPerson.isBlank()) {
            _state.update { it.copy(editContactPersonError = "Contact person is required") }; valid = false
        }
        return valid
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
                    _state.update { it.copy(isChangingPassword = false, error = error.message ?: "Failed to change password") }
                }
            )
        }
    }
}