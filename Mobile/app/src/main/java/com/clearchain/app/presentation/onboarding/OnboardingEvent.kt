package com.clearchain.app.presentation.onboarding

import android.net.Uri

sealed class OnboardingEvent {
    // Step 1 field changes
    data class PhoneChanged(val value: String) : OnboardingEvent()
    data class DescriptionChanged(val value: String) : OnboardingEvent()
    data class ContactPersonChanged(val value: String) : OnboardingEvent()

    // Step 2 field changes
    data class AddressChanged(val value: String) : OnboardingEvent()
    data class CityChanged(val value: String) : OnboardingEvent()
    data class StateChanged(val value: String) : OnboardingEvent()
    data class ZipCodeChanged(val value: String) : OnboardingEvent()
    data class OpenTimeChanged(val value: String) : OnboardingEvent()
    data class CloseTimeChanged(val value: String) : OnboardingEvent()
    data class PickupInstructionsChanged(val value: String) : OnboardingEvent()
    data class AddressSelected(
        val address: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val lat: Double,
        val lng: Double
    ) : OnboardingEvent()

    // Document upload
    data class DocumentSelected(val uri: Uri, val name: String) : OnboardingEvent()
    object RemoveDocument : OnboardingEvent()

    // Navigation between steps
    object NextStep : OnboardingEvent()
    object PreviousStep : OnboardingEvent()
    object FinishOnboarding : OnboardingEvent()
}
