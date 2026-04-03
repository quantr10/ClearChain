package com.clearchain.app.presentation.onboarding

sealed class OnboardingEvent {
    // Step 1 field changes
    data class PhoneChanged(val value: String) : OnboardingEvent()
    data class DescriptionChanged(val value: String) : OnboardingEvent()
    data class ContactPersonChanged(val value: String) : OnboardingEvent()

    // Step 2 field changes
    data class AddressChanged(val value: String) : OnboardingEvent()
    data class CityChanged(val value: String) : OnboardingEvent()
    data class OpenTimeChanged(val value: String) : OnboardingEvent()
    data class CloseTimeChanged(val value: String) : OnboardingEvent()
    data class PickupInstructionsChanged(val value: String) : OnboardingEvent()
    data class AddressSelected(val address: String, val city: String, val lat: Double, val lng: Double) : OnboardingEvent()

    // Navigation between steps
    object NextStep : OnboardingEvent()          // validate → save API → advance
    object PreviousStep : OnboardingEvent()      // go back one step (no API call)
    object FinishOnboarding : OnboardingEvent()  // Step 3 "Get Started" → navigate to Dashboard
}