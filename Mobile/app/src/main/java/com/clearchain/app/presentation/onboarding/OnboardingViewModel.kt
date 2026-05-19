package com.clearchain.app.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.OnboardingDraftStore
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.profile.UpdateProfileUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val draftStore: OnboardingDraftStore
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
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
                        addressLat = user.latitude,
                        addressLng = user.longitude,
                        city = user.location,
                        openTime = user.hours?.substringBefore(" - ", "") ?: "",
                        closeTime = user.hours?.substringAfter(" - ", "") ?: "",
                        pickupInstructions = user.pickupInstructions ?: ""
                    )
                }
            }

            // Check for saved draft
            val draft = draftStore.draft.first()
            if (draft != null && draft.savedAt > 0) {
                _state.update { it.copy(hasSavedDraft = true, showDraftRecoveryDialog = true) }
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
            is OnboardingEvent.OpenTimeChanged ->
                _state.update { it.copy(openTime = event.value) }
            is OnboardingEvent.CloseTimeChanged ->
                _state.update { it.copy(closeTime = event.value) }
            is OnboardingEvent.PickupInstructionsChanged ->
                _state.update { it.copy(pickupInstructions = event.value) }
            is OnboardingEvent.AddressSelected -> {
                _state.update {
                    it.copy(
                        address = event.address,
                        city = event.city.ifBlank { it.city },
                        addressLat = event.lat,
                        addressLng = event.lng,
                        addressError = null,
                        cityError = null
                    )
                }
            }
            is OnboardingEvent.DocumentSelected ->
                _state.update {
                    it.copy(
                        verificationDocumentUri = event.uri,
                        verificationDocumentName = event.name,
                        documentUploadError = null
                    )
                }
            OnboardingEvent.RemoveDocument ->
                _state.update {
                    it.copy(verificationDocumentUri = null, verificationDocumentName = null)
                }
            OnboardingEvent.SaveDraft -> saveDraft()
            OnboardingEvent.RestoreDraft -> restoreDraft()
            OnboardingEvent.DismissDraftDialog ->
                _state.update { it.copy(showDraftRecoveryDialog = false) }
            OnboardingEvent.NextStep -> handleNextStep()
            OnboardingEvent.PreviousStep -> handlePreviousStep()
            OnboardingEvent.FinishOnboarding -> {
                viewModelScope.launch {
                    draftStore.clear()
                    _uiEvent.send(UiEvent.Navigate("dashboard"))
                }
            }
        }
    }

    private fun saveDraft(showConfirmation: Boolean = true) {
        val s = _state.value
        viewModelScope.launch {
            draftStore.save(
                com.clearchain.app.data.local.OnboardingDraft(
                    phone = s.phone,
                    description = s.description,
                    contactPerson = s.contactPerson,
                    address = s.address,
                    city = s.city,
                    openTime = s.openTime,
                    closeTime = s.closeTime,
                    pickupInstructions = s.pickupInstructions,
                    verificationDocumentUri = s.verificationDocumentUri?.toString() ?: ""
                )
            )
            if (showConfirmation) _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_draft_saved)))
        }
    }

    private fun restoreDraft() {
        viewModelScope.launch {
            val draft = draftStore.draft.first() ?: return@launch
            _state.update {
                it.copy(
                    phone = draft.phone.ifBlank { it.phone },
                    description = draft.description.ifBlank { it.description },
                    contactPerson = draft.contactPerson.ifBlank { it.contactPerson },
                    address = draft.address.ifBlank { it.address },
                    city = draft.city.ifBlank { it.city },
                    openTime = draft.openTime.ifBlank { it.openTime },
                    closeTime = draft.closeTime.ifBlank { it.closeTime },
                    pickupInstructions = draft.pickupInstructions.ifBlank { it.pickupInstructions },
                    showDraftRecoveryDialog = false,
                    hasSavedDraft = false
                )
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_draft_restored)))
        }
    }

    private fun handleNextStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> {
                var valid = true
                if (s.phone.isBlank()) {
                    _state.update { it.copy(phoneError = context.getString(R.string.error_phone_required)) }
                    valid = false
                }
                if ((s.userType == OrganizationType.NGO || s.userType == OrganizationType.GROCERY) && s.contactPerson.isBlank()) {
                    _state.update { it.copy(contactPersonError = context.getString(R.string.error_contact_person_required)) }
                    valid = false
                }
                if (!valid) return
                saveAndAdvance()
            }
            2 -> {
                var valid = true
                if (s.address.isBlank()) {
                    _state.update { it.copy(addressError = context.getString(R.string.error_address_required)) }
                    valid = false
                }
                if (s.city.isBlank()) {
                    _state.update { it.copy(cityError = context.getString(R.string.error_city_required)) }
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
                hours = if (s.openTime.isNotBlank() && s.closeTime.isNotBlank())
                    "${s.openTime} - ${s.closeTime}" else null,
                latitude = s.addressLat,
                longitude = s.addressLng,
                contactPerson = s.contactPerson.ifBlank { null },
                pickupInstructions = s.pickupInstructions.ifBlank { null },
                description = s.description.ifBlank { null },
            )

            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false, currentStep = it.currentStep + 1) }
                    // Auto-save draft so progress is preserved if the user exits mid-flow
                    saveDraft(showConfirmation = false)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isSaving = false, error = error.message ?: context.getString(R.string.error_onboarding_save_failed))
                    }
                }
            )
        }
    }
}
