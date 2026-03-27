package com.clearchain.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.profile.UpdateProfileUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Load current user to pre-fill fields (e.g. app killed mid-wizard)
        viewModelScope.launch {
            val user = getCurrentUserUseCase().first()
            if (user != null) {
                _state.update {
                    it.copy(
                        userType = user.type,
                        userName = user.name,
                        phone = user.phone,
                        description = user.description ?: "",
                        contactPerson = user.contactPerson ?: "",
                        address = user.address,
                        city = user.location,
                        hours = user.hours ?: "",
                        pickupInstructions = user.pickupInstructions ?: ""
                    )
                }
            }
        }
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.PhoneChanged ->
                _state.update { it.copy(phone = event.value, phoneError = null) }
            is OnboardingEvent.DescriptionChanged ->
                _state.update { it.copy(description = event.value) }
            is OnboardingEvent.ContactPersonChanged ->
                _state.update { it.copy(contactPerson = event.value, contactPersonError = null) }
            is OnboardingEvent.AddressChanged ->
                _state.update { it.copy(address = event.value, addressError = null) }
            is OnboardingEvent.CityChanged ->
                _state.update { it.copy(city = event.value, cityError = null) }
            is OnboardingEvent.HoursChanged ->
                _state.update { it.copy(hours = event.value) }
            is OnboardingEvent.PickupInstructionsChanged ->
                _state.update { it.copy(pickupInstructions = event.value) }
            OnboardingEvent.NextStep -> handleNextStep()
            OnboardingEvent.PreviousStep -> handlePreviousStep()
            OnboardingEvent.FinishOnboarding -> {
                viewModelScope.launch { _uiEvent.send(UiEvent.Navigate("dashboard")) }
            }
        }
    }

    private fun handleNextStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> {
                var valid = true
                if (s.phone.isBlank()) {
                    _state.update { it.copy(phoneError = "Phone number is required") }
                    valid = false
                }
                if ((s.userType == OrganizationType.NGO || s.userType == OrganizationType.GROCERY) && s.contactPerson.isBlank()) {
                    _state.update { it.copy(contactPersonError = "Contact person is required") }
                    valid = false
                }
                if (!valid) return
                saveAndAdvance()
            }
            2 -> {
                var valid = true
                if (s.address.isBlank()) {
                    _state.update { it.copy(addressError = "Address is required") }
                    valid = false
                }
                if (s.city.isBlank()) {
                    _state.update { it.copy(cityError = "City is required") }
                    valid = false
                }
                if (!valid) return
                saveAndAdvance()
            }
        }
    }

    private fun handlePreviousStep() {
        val current = _state.value.currentStep
        if (current > 1) _state.update { it.copy(currentStep = current - 1) }
    }

    private fun saveAndAdvance() {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            val result = updateProfileUseCase(
                name = s.userName,
                phone = s.phone,
                address = s.address,
                location = s.city,
                hours = s.hours.ifBlank { null },
                contactPerson = s.contactPerson.ifBlank { null },
                pickupInstructions = s.pickupInstructions.ifBlank { null },
                description = s.description.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false, currentStep = it.currentStep + 1) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isSaving = false, error = error.message ?: "Failed to save. Please try again.")
                    }
                }
            )
        }
    }
}