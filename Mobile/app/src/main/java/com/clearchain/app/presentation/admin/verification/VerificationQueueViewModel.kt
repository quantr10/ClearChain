package com.clearchain.app.presentation.admin.verification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.RejectOrganizationBody
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.util.DownloadsExport
import com.clearchain.app.util.BulkResult
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class VerificationQueueViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adminApi: AdminApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(VerificationQueueState())
    val state: StateFlow<VerificationQueueState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadOrganizations()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch {
            signalRService.newOrganizationRegistered.collect { notification ->
                loadOrganizations()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_registration, notification.type, notification.name)))
            }
        }
    }

    fun onEvent(event: VerificationQueueEvent) {
        // While an action is in flight, the list and the selection it started from stay put.
        if (_state.value.isProcessing && event.isLockedWhileBusy()) return
        when (event) {
            VerificationQueueEvent.RefreshOrganizations -> refreshOrganizations()

            is VerificationQueueEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query) }
            is VerificationQueueEvent.StatusFilterChanged ->
                _state.update { it.copy(selectedStatus = event.status) }
            is VerificationQueueEvent.SortOptionChanged ->
                _state.update { it.copy(selectedSort = event.option) }

            // Advanced filter sheet
            VerificationQueueEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            VerificationQueueEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is VerificationQueueEvent.FilterOrgTypeChanged ->
                _state.update { it.copy(filterOrgType = event.type) }
            VerificationQueueEvent.ClearAdvancedFilters ->
                _state.update { it.copy(filterOrgType = null) }

            // Approval checklist
            is VerificationQueueEvent.ShowChecklist ->
                _state.update { it.copy(showChecklistForId = event.orgId, checkedItems = emptySet()) }
            VerificationQueueEvent.DismissChecklist ->
                _state.update { it.copy(showChecklistForId = null) }
            is VerificationQueueEvent.ToggleChecklistItem ->
                _state.update {
                    val updated = if (event.index in it.checkedItems) {
                        it.checkedItems - event.index
                    } else {
                        it.checkedItems + event.index
                    }
                    it.copy(checkedItems = updated)
                }
            VerificationQueueEvent.ConfirmApprove -> approveOrganization()

            // Rejection dialog
            is VerificationQueueEvent.ShowRejectDialog ->
                _state.update { it.copy(showRejectDialogForId = event.orgId, rejectionReason = "") }
            VerificationQueueEvent.DismissRejectDialog ->
                _state.update { it.copy(showRejectDialogForId = null) }
            is VerificationQueueEvent.RejectionReasonChanged ->
                _state.update { it.copy(rejectionReason = event.reason) }
            is VerificationQueueEvent.SelectRejectionTemplate ->
                _state.update { it.copy(rejectionReason = event.reason) }
            VerificationQueueEvent.ConfirmReject -> rejectOrganization()

            // Batch selection
            VerificationQueueEvent.ToggleBatchMode ->
                _state.update { it.copy(isBatchMode = !it.isBatchMode, selectedOrgIds = emptySet()) }
            is VerificationQueueEvent.ToggleOrgSelection -> {
                _state.update {
                    val updated = if (event.orgId in it.selectedOrgIds) {
                        it.selectedOrgIds - event.orgId
                    } else {
                        it.selectedOrgIds + event.orgId
                    }
                    it.copy(selectedOrgIds = updated, isBatchMode = updated.isNotEmpty())
                }
            }
            VerificationQueueEvent.SelectAllVisible ->
                _state.update {
                    val selected = it.filteredOrgs.map { o -> o.id }.toSet()
                    it.copy(selectedOrgIds = selected, isBatchMode = selected.isNotEmpty())
                }
            VerificationQueueEvent.ClearSelection ->
                _state.update { it.copy(selectedOrgIds = emptySet(), isBatchMode = false) }
            VerificationQueueEvent.BatchApprove -> batchApprove()
            VerificationQueueEvent.BatchReject -> batchReject()

            VerificationQueueEvent.ExportCsv -> exportCsv()
        }
    }

    private fun exportCsv() {
        val current = _state.value
        val orgs = current.filteredOrgs
        if (orgs.isEmpty()) {
            viewModelScope.launch { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_no_orgs_export))) }
            return
        }
        val tag = current.selectedStatus?.lowercase() ?: "all"
        viewModelScope.launch {
            val message = DownloadsExport.exportCsv(
                context,
                filePrefix = "clearchain_organizations_$tag",
                chooserTitle = "Share Organizations CSV",
                header = listOf("Name", "Type", "Status", "Email", "Phone", "Location", "Contact Person", "Created At"),
                rows = orgs.map { org ->
                    listOf(
                        org.name,
                        org.type.name,
                        org.verificationStatus.name,
                        org.email,
                        org.phone,
                        org.location,
                        org.contactPerson ?: "",
                        org.createdAt.take(10)
                    )
                }
            )
            _uiEvent.send(UiEvent.ShowSnackbar(message))
        }
    }

    private fun batchApprove() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            // The batch bar stays up (Approve All spinning) until every call settles.
            _state.update { it.copy(isProcessing = true, batchOperation = VerificationBatchOperation.APPROVE) }
            var successCount = 0
            ids.forEach { orgId ->
                try {
                    adminApi.verifyOrganization(orgId)
                    successCount++
                } catch (_: Exception) {}
            }
            _state.update { it.copy(isProcessing = false, batchOperation = null, isBatchMode = false, selectedOrgIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(BulkResult.message(context, successCount, ids.size, R.plurals.snack_n_orgs_approved)))
            loadOrganizations()
        }
    }

    private fun batchReject() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, batchOperation = VerificationBatchOperation.REJECT) }
            var successCount = 0
            ids.forEach { orgId ->
                try {
                    adminApi.unverifyOrganization(orgId, RejectOrganizationBody())
                    successCount++
                } catch (_: Exception) {}
            }
            _state.update { it.copy(isProcessing = false, batchOperation = null, isBatchMode = false, selectedOrgIds = emptySet()) }
            _uiEvent.send(UiEvent.ShowSnackbar(BulkResult.message(context, successCount, ids.size, R.plurals.snack_n_orgs_rejected)))
            loadOrganizations()
        }
    }

    private fun approveOrganization() {
        val orgId = _state.value.showChecklistForId ?: return
        viewModelScope.launch {
            // The checklist dialog stays open (confirm spinning) until the call settles.
            _state.update { it.copy(isProcessing = true) }
            try {
                adminApi.verifyOrganization(orgId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_approved)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_approve_failed)))
            }
            _state.update { it.copy(isProcessing = false, showChecklistForId = null) }
        }
    }

    private fun rejectOrganization() {
        val orgId = _state.value.showRejectDialogForId ?: return
        val reason = _state.value.rejectionReason.trim().ifBlank { null }
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            try {
                adminApi.unverifyOrganization(orgId, RejectOrganizationBody(reason))
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_rejected)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_reject_failed)))
            }
            _state.update { it.copy(isProcessing = false, showRejectDialogForId = null) }
        }
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data
                    .map { dto -> dto.toDomain() }
                    .filter { it.type != OrganizationType.ADMIN }
                _state.update { it.copy(organizations = organizations, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_load_orgs)))
            }
        }
    }

    private fun refreshOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data
                    .map { dto -> dto.toDomain() }
                    .filter { it.type != OrganizationType.ADMIN }
                _state.update { it.copy(organizations = organizations, isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_orgs_refreshed)))
            } catch (e: Exception) {
                _state.update { it.copy(isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_refresh_failed)))
            }
        }
    }

    private fun VerificationQueueEvent.isLockedWhileBusy(): Boolean = when (this) {
            is VerificationQueueEvent.ToggleOrgSelection,
            is VerificationQueueEvent.SelectAllVisible,
            is VerificationQueueEvent.ClearSelection,
            is VerificationQueueEvent.ToggleBatchMode,
            is VerificationQueueEvent.StatusFilterChanged,
            is VerificationQueueEvent.FilterOrgTypeChanged,
            is VerificationQueueEvent.SearchQueryChanged,
            is VerificationQueueEvent.SortOptionChanged,
            is VerificationQueueEvent.ClearAdvancedFilters,
            is VerificationQueueEvent.ShowFilterSheet,
            is VerificationQueueEvent.RefreshOrganizations,
            is VerificationQueueEvent.ShowChecklist,
            is VerificationQueueEvent.ShowRejectDialog,
            is VerificationQueueEvent.ToggleChecklistItem,
            is VerificationQueueEvent.RejectionReasonChanged,
            is VerificationQueueEvent.SelectRejectionTemplate,
            is VerificationQueueEvent.BatchApprove,
            is VerificationQueueEvent.BatchReject,
            is VerificationQueueEvent.ConfirmApprove,
            is VerificationQueueEvent.ConfirmReject -> true
        else -> false
    }
}
