package com.clearchain.app.presentation.admin.verification

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        viewModelScope.launch { signalRService.connect() }
        viewModelScope.launch {
            signalRService.newOrganizationRegistered.collect { notification ->
                loadOrganizations()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_registration, notification.type, notification.name)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { signalRService.disconnect() }
    }

    fun onEvent(event: VerificationQueueEvent) {
        when (event) {
            VerificationQueueEvent.LoadOrganizations -> loadOrganizations()
            VerificationQueueEvent.RefreshOrganizations -> refreshOrganizations()
            VerificationQueueEvent.ClearError -> _state.update { it.copy(error = null) }

            is VerificationQueueEvent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = event.query) }
            is VerificationQueueEvent.StatusFilterChanged ->
                _state.update { it.copy(selectedStatus = event.status) }

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
                    val updated = if (event.index in it.checkedItems)
                        it.checkedItems - event.index else it.checkedItems + event.index
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
                    val updated = if (event.orgId in it.selectedOrgIds)
                        it.selectedOrgIds - event.orgId else it.selectedOrgIds + event.orgId
                    it.copy(selectedOrgIds = updated)
                }
            }
            VerificationQueueEvent.SelectAllVisible ->
                _state.update { it.copy(selectedOrgIds = it.filteredOrgs.map { o -> o.id }.toSet()) }
            VerificationQueueEvent.ClearSelection ->
                _state.update { it.copy(selectedOrgIds = emptySet()) }
            VerificationQueueEvent.BatchApprove -> batchApprove()
            VerificationQueueEvent.BatchReject  -> batchReject()
        }
    }

    private fun batchApprove() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, isBatchMode = false, selectedOrgIds = emptySet()) }
            var successCount = 0
            ids.forEach { orgId ->
                try { adminApi.verifyOrganization(orgId); successCount++ } catch (_: Exception) {}
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_approved_orgs, successCount, ids.size)))
            loadOrganizations()
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun batchReject() {
        val ids = _state.value.selectedOrgIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, isBatchMode = false, selectedOrgIds = emptySet()) }
            var successCount = 0
            ids.forEach { orgId ->
                try { adminApi.unverifyOrganization(orgId); successCount++ } catch (_: Exception) {}
            }
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_rejected_orgs, successCount, ids.size)))
            loadOrganizations()
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun approveOrganization() {
        val orgId = _state.value.showChecklistForId ?: return
        viewModelScope.launch {
            _state.update { it.copy(showChecklistForId = null, isProcessing = true) }
            try {
                adminApi.verifyOrganization(orgId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_approved)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_approve_failed)))
            }
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun rejectOrganization() {
        val orgId = _state.value.showRejectDialogForId ?: return
        viewModelScope.launch {
            _state.update { it.copy(showRejectDialogForId = null, isProcessing = true) }
            try {
                adminApi.unverifyOrganization(orgId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_org_rejected)))
                loadOrganizations()
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_reject_failed)))
            }
            _state.update { it.copy(isProcessing = false) }
        }
    }

    private fun loadOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data.map { dto -> dto.toDomain() }
                _state.update { it.copy(organizations = organizations, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_load_orgs), isLoading = false) }
            }
        }
    }

    private fun refreshOrganizations() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            try {
                val response = adminApi.getAllOrganizations()
                val organizations = response.data.map { dto -> dto.toDomain() }
                _state.update { it.copy(organizations = organizations, isRefreshing = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_orgs_refreshed)))
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_refresh_failed), isRefreshing = false) }
            }
        }
    }
}
