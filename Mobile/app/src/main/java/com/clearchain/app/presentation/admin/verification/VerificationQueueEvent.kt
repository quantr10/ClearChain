package com.clearchain.app.presentation.admin.verification

sealed class VerificationQueueEvent {
    object LoadOrganizations : VerificationQueueEvent()
    object RefreshOrganizations : VerificationQueueEvent()
    data class VerifyOrganization(val organizationId: String) : VerificationQueueEvent()
    object ClearError : VerificationQueueEvent()
}