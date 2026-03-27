package com.clearchain.app.presentation.onboarding

sealed class OnboardingEvent {
    // Step 1 field changes
    data class PhoneChanged(val value: String) : OnboardingEvent()
    data class DescriptionChanged(val value: String) : OnboardingEvent()
    data class ContactPersonChanged(val value: String) : OnboardingEvent()  // NGO only

    // Step 2 field changes
    data class AddressChanged(val value: String) : OnboardingEvent()
    data class CityChanged(val value: String) : OnboardingEvent()
    data class HoursChanged(val value: String) : OnboardingEvent()
    data class PickupInstructionsChanged(val value: String) : OnboardingEvent()  // Grocery only

    // Navigation between steps
    object NextStep : OnboardingEvent()          // validate → save API → advance
    object PreviousStep : OnboardingEvent()      // go back one step (no API call)
    object FinishOnboarding : OnboardingEvent()  // Step 3 "Get Started" → navigate to Dashboard
}