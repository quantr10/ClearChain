package com.clearchain.app.presentation.admin.disputes

sealed class AdminDisputesEvent {
    object LoadDisputes : AdminDisputesEvent()
    object RefreshDisputes : AdminDisputesEvent()
    object ClearError : AdminDisputesEvent()

    data class StatusFilterChanged(val status: String?) : AdminDisputesEvent()
    data class ToggleExpanded(val disputeId: String) : AdminDisputesEvent()

    data class ShowResolveDialog(val disputeId: String) : AdminDisputesEvent()
    object DismissResolveDialog : AdminDisputesEvent()
    data class OutcomeChanged(val outcome: String) : AdminDisputesEvent()
    data class GroceryStatementChanged(val text: String) : AdminDisputesEvent()
    data class ResolveNoteChanged(val text: String) : AdminDisputesEvent()
    object ConfirmResolve : AdminDisputesEvent()
}
