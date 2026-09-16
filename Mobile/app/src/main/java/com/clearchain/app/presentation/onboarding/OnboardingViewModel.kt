package com.clearchain.app.presentation.onboarding

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.profile.UpdateProfileUseCase
import com.clearchain.app.domain.usecase.profile.UploadVerificationDocumentUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val uploadVerificationDocumentUseCase: UploadVerificationDocumentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase().first()
            if (user != null) {
                val openTime = user.hours?.substringBefore(" - ", "") ?: ""
                val closeTime = user.hours?.substringAfter(" - ", "") ?: ""
                _state.update {
                    it.copy(
                        userType = user.type,
                        userName = user.name,
                        currentStep = startingStepFor(
                            type = user.type,
                            phone = user.phone,
                            contactPerson = user.contactPerson ?: "",
                            address = user.address,
                            city = user.location,
                            state = user.state ?: "",
                            zipCode = user.zipCode ?: "",
                            openTime = openTime,
                            closeTime = closeTime,
                            hasDocument = user.documentUrl != null,
                            // A rejected org is here to fix something — always restart at
                            // step 1 instead of skipping straight to the finished screen,
                            // even though every field is technically already filled in.
                            forceRestart = user.verificationStatus == VerificationStatus.REJECTED
                        ),
                        phone = user.phone,
                        description = user.description ?: "",
                        contactPerson = user.contactPerson ?: "",
                        address = user.address,
                        addressLat = user.latitude,
                        addressLng = user.longitude,
                        city = user.location,
                        state = user.state ?: "",
                        zipCode = user.zipCode ?: "",
                        openTime = openTime,
                        closeTime = closeTime,
                        pickupInstructions = user.pickupInstructions ?: "",
                        uploadedDocumentUrl = user.documentUrl,
                        verificationDocumentName = user.documentUrl?.substringAfterLast('/')
                    )
                }
            }
        }
    }

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.PhoneChanged ->
                _state.update { it.copy(phone = event.value, phoneError = null, error = null) }
            is OnboardingEvent.DescriptionChanged ->
                _state.update { it.copy(description = event.value) }
            is OnboardingEvent.ContactPersonChanged ->
                _state.update { it.copy(contactPerson = event.value, contactPersonError = null, error = null) }
            is OnboardingEvent.AddressChanged ->
                _state.update { it.copy(address = event.value, addressError = null) }
            is OnboardingEvent.CityChanged ->
                _state.update { it.copy(city = event.value, cityError = null) }
            is OnboardingEvent.StateChanged ->
                _state.update { it.copy(state = event.value, stateError = null) }
            is OnboardingEvent.ZipCodeChanged ->
                _state.update { it.copy(zipCode = event.value, zipCodeError = null) }
            is OnboardingEvent.OpenTimeChanged ->
                _state.update { it.copy(openTime = event.value, openTimeError = null, error = null) }
            is OnboardingEvent.CloseTimeChanged ->
                _state.update { it.copy(closeTime = event.value, closeTimeError = null, error = null) }
            is OnboardingEvent.PickupInstructionsChanged ->
                _state.update { it.copy(pickupInstructions = event.value) }
            is OnboardingEvent.AddressSelected -> {
                _state.update {
                    it.copy(
                        address = event.address,
                        city = event.city.ifBlank { it.city },
                        state = event.state.ifBlank { it.state },
                        zipCode = event.zipCode.ifBlank { it.zipCode },
                        addressLat = event.lat,
                        addressLng = event.lng,
                        addressError = null,
                        cityError = null,
                        stateError = null,
                        zipCodeError = null
                    )
                }
            }
            is OnboardingEvent.DocumentSelected -> {
                _state.update {
                    it.copy(
                        verificationDocumentUri = event.uri,
                        verificationDocumentName = event.name,
                        documentUploadError = null
                    )
                }
                uploadDocument(event.uri)
            }
            OnboardingEvent.RemoveDocument ->
                _state.update {
                    it.copy(
                        verificationDocumentUri = null,
                        verificationDocumentName = null,
                        uploadedDocumentUrl = null,
                        documentUploadError = null
                    )
                }
            OnboardingEvent.NextStep -> handleNextStep()
            OnboardingEvent.PreviousStep -> handlePreviousStep()
            OnboardingEvent.FinishOnboarding -> {
                // Route string is unused by OnboardingScreen (any Navigate event just calls
                // onFinished(), which always goes through Splash) — named for what actually
                // happens next: Splash re-checks verification status and gates accordingly.
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.Navigate("splash_recheck"))
                }
            }
        }
    }

    private fun handleNextStep() {
        val s = _state.value
        when (s.currentStep) {
            1 -> {
                _state.update { it.copy(error = null) }
                var valid = true
                val phoneError = when {
                    s.phone.isBlank() -> context.getString(R.string.error_phone_required)
                    !isValidOnboardingPhone(s.phone) -> context.getString(R.string.error_phone_invalid)
                    else -> null
                }
                if (phoneError != null) {
                    _state.update { it.copy(phoneError = phoneError, error = null) }
                    viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(phoneError)) }
                    valid = false
                }
                if (s.userType == OrganizationType.NGO || s.userType == OrganizationType.GROCERY) {
                    val contactError = when {
                        s.contactPerson.isBlank() -> context.getString(R.string.error_contact_person_required)
                        !isValidContactPerson(s.contactPerson) -> context.getString(R.string.error_contact_person_invalid)
                        else -> null
                    }
                    if (contactError != null) {
                        _state.update { it.copy(contactPersonError = contactError, error = null) }
                        viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(contactError)) }
                        valid = false
                    }
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
                if (s.state.isBlank()) {
                    _state.update { it.copy(stateError = context.getString(R.string.error_state_required)) }
                    valid = false
                }
                if (s.zipCode.isBlank()) {
                    _state.update { it.copy(zipCodeError = context.getString(R.string.error_zip_code_required)) }
                    valid = false
                }
                if (s.openTime.isBlank()) {
                    _state.update { it.copy(openTimeError = context.getString(R.string.error_opening_time_required)) }
                    valid = false
                }
                if (s.closeTime.isBlank()) {
                    _state.update { it.copy(closeTimeError = context.getString(R.string.error_closing_time_required)) }
                    valid = false
                }
                if (s.isUploadingDocument) {
                    viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.onboarding_doc_uploading))) }
                    valid = false
                } else if (s.uploadedDocumentUrl == null) {
                    val msg = context.getString(R.string.error_document_required)
                    _state.update { it.copy(documentUploadError = msg) }
                    viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(msg)) }
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
                state = s.state,
                zipCode = s.zipCode,
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
                },
                onFailure = { error ->
                    val msg = error.message ?: context.getString(R.string.error_onboarding_save_failed)
                    _state.update { it.copy(isSaving = false, error = msg) }
                }
            )
        }
    }

    private fun uploadDocument(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingDocument = true, documentUploadError = null) }
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                } ?: uri.lastPathSegment ?: "document"

                val bytes = withContext(Dispatchers.IO) {
                    resolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException(context.getString(R.string.error_document_read_failed))

                uploadVerificationDocumentUseCase(bytes, displayName, mimeType).fold(
                    onSuccess = { url ->
                        _state.update {
                            it.copy(
                                isUploadingDocument = false,
                                uploadedDocumentUrl = url,
                                verificationDocumentName = displayName,
                                documentUploadError = null
                            )
                        }
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.onboarding_doc_uploaded)))
                    },
                    onFailure = { e ->
                        _state.update {
                            it.copy(
                                isUploadingDocument = false,
                                uploadedDocumentUrl = null,
                                documentUploadError = e.message ?: context.getString(R.string.error_document_upload_failed)
                            )
                        }
                        _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_document_upload_failed)))
                    }
                )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isUploadingDocument = false,
                        documentUploadError = e.message ?: context.getString(R.string.error_document_upload_failed)
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_document_upload_failed)))
            }
        }
    }

    private fun startingStepFor(
        type: OrganizationType,
        phone: String,
        contactPerson: String,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        openTime: String,
        closeTime: String,
        hasDocument: Boolean,
        forceRestart: Boolean = false
    ): Int {
        if (forceRestart) return 1

        val stepOneComplete = phone.isNotBlank() &&
            (type !in listOf(OrganizationType.NGO, OrganizationType.GROCERY) || contactPerson.isNotBlank())
        val stepTwoComplete = address.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank() &&
            zipCode.isNotBlank() &&
            openTime.isNotBlank() &&
            closeTime.isNotBlank() &&
            hasDocument

        return when {
            !stepOneComplete -> 1
            !stepTwoComplete -> 2
            else -> 3
        }
    }

    private fun isValidOnboardingPhone(phone: String): Boolean {
        val trimmed = phone.trim()
        val digits = trimmed.filter(Char::isDigit)
        return trimmed.startsWith("+") &&
            digits.length in 10..15 &&
            trimmed.all { it.isDigit() || it in setOf('+', ' ', '-', '(', ')') }
    }

    private fun isValidContactPerson(name: String): Boolean {
        val parts = name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        return parts.size >= 2 &&
            parts.all { part ->
                part.length >= 2 && part.all { it.isLetter() || it == '\'' || it == '-' || it == '.' }
            }
    }
}
