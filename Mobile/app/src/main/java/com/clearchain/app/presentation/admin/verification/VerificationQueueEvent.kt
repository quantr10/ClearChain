package com.clearchain.app.presentation.admin.verification

sealed class VerificationQueueEvent {
    object LoadOrganizations : VerificationQueueEvent()
    object RefreshOrganizations : VerificationQueueEvent()
    object ClearError : VerificationQueueEvent()

    data class SearchQueryChanged(val query: String) : VerificationQueueEvent()
    data class StatusFilterChanged(val status: String?) : VerificationQueueEvent()

    // Advanced filter sheet
    object ShowFilterSheet : VerificationQueueEvent()
    object HideFilterSheet : VerificationQueueEvent()
    data class FilterOrgTypeChanged(val type: String?) : VerificationQueueEvent()
    object ClearAdvancedFilters : VerificationQueueEvent()

    // Approval flow with checklist
    data class ShowChecklist(val orgId: String) : VerificationQueueEvent()
    object DismissChecklist : VerificationQueueEvent()
    data class ToggleChecklistItem(val index: Int) : VerificationQueueEvent()
    object ConfirmApprove : VerificationQueueEvent()

    // Rejection flow with template reasons
    data class ShowRejectDialog(val orgId: String) : VerificationQueueEvent()
    object DismissRejectDialog : VerificationQueueEvent()
    data class RejectionReasonChanged(val reason: String) : VerificationQueueEvent()
    data class SelectRejectionTemplate(val reason: String) : VerificationQueueEvent()
    object ConfirmReject : VerificationQueueEvent()

    // Batch selection mode
    object ToggleBatchMode : VerificationQueueEvent()
    data class ToggleOrgSelection(val orgId: String) : VerificationQueueEvent()
    object SelectAllVisible : VerificationQueueEvent()
    object ClearSelection : VerificationQueueEvent()
    object BatchApprove : VerificationQueueEvent()
    object BatchReject : VerificationQueueEvent()
}
