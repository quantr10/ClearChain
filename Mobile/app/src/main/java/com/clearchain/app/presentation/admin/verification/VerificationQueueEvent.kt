package com.clearchain.app.presentation.admin.verification

sealed class VerificationQueueEvent {
    object LoadOrganizations : VerificationQueueEvent()
    object RefreshOrganizations : VerificationQueueEvent()
    object ClearError : VerificationQueueEvent()
}