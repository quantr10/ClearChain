package com.clearchain.app.presentation.grocery.managerequests

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.BulkActionRequest
import com.clearchain.app.data.remote.dto.BulkRejectRequest
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.usecase.pickuprequest.ApprovePickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.GetGroceryPickupRequestsUseCase
import com.clearchain.app.domain.usecase.pickuprequest.MarkReadyForPickupUseCase
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRequestsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getGroceryPickupRequestsUseCase: GetGroceryPickupRequestsUseCase,
    private val approvePickupRequestUseCase: ApprovePickupRequestUseCase,
    private val cancelPickupRequestUseCase: CancelPickupRequestUseCase,
    private val markReadyForPickupUseCase: MarkReadyForPickupUseCase,
    private val pickupRequestApi: PickupRequestApi,
    private val organizationApi: OrganizationApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(ManageRequestsState())
    val state: StateFlow<ManageRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadRequests()
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }

        viewModelScope.launch {
            signalRService.pickupRequestCreated.collect { request ->
                loadRequests()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_request_from, request.ngoName)))
            }
        }
        viewModelScope.launch {
            signalRService.pickupRequestStatusChanged.collect { loadRequests() }
        }
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { request ->
                loadRequests()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled_by, request.ngoName)))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { signalRService.disconnect() }
    }

    fun onEvent(event: ManageRequestsEvent) {
        when (event) {
            ManageRequestsEvent.LoadRequests -> loadRequests()
            ManageRequestsEvent.RefreshRequests -> refreshRequests()

            is ManageRequestsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is ManageRequestsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is ManageRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            // Advanced filter sheet
            ManageRequestsEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            ManageRequestsEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is ManageRequestsEvent.FilterCategoryChanged -> {
                _state.update { it.copy(filterCategory = event.category) }
                applyFilters()
            }
            is ManageRequestsEvent.FilterPickupDatePresetChanged -> {
                _state.update { it.copy(filterPickupDatePreset = event.preset) }
                applyFilters()
            }
            ManageRequestsEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(filterCategory = null, filterPickupDatePreset = null) }
                applyFilters()
            }

            is ManageRequestsEvent.ApproveRequest -> approveRequest(event.requestId)
            is ManageRequestsEvent.RejectRequest -> rejectRequest(event.requestId)
            is ManageRequestsEvent.MarkReady -> markReadyForPickup(event.requestId)

            // Bulk selection
            ManageRequestsEvent.ToggleSelectionMode ->
                _state.update { it.copy(isSelectionMode = !it.isSelectionMode, selectedIds = emptySet()) }
            is ManageRequestsEvent.ToggleItemSelection ->
                _state.update {
                    val updated = if (event.requestId in it.selectedIds)
                        it.selectedIds - event.requestId
                    else
                        it.selectedIds + event.requestId
                    it.copy(selectedIds = updated)
                }
            ManageRequestsEvent.SelectAll ->
                _state.update { it.copy(selectedIds = it.filteredRequests.map { r -> r.id }.toSet()) }
            ManageRequestsEvent.DeselectAll ->
                _state.update { it.copy(selectedIds = emptySet()) }
            ManageRequestsEvent.BulkApprove -> bulkApprove()
            is ManageRequestsEvent.BulkReject -> bulkReject(event.reason)

            ManageRequestsEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getGroceryPickupRequestsUseCase().fold(
                onSuccess = { requests ->
                    _state.update { it.copy(allRequests = requests, isLoading = false) }
                    applyFilters()
                    fetchNgoReputations(requests.map { it.ngoId }.toSet())
                },
                onFailure = { error ->
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load requests") }
                }
            )
        }
    }

    private fun refreshRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            getGroceryPickupRequestsUseCase().fold(
                onSuccess = { requests ->
                    _state.update { it.copy(allRequests = requests, isRefreshing = false) }
                    applyFilters()
                    fetchNgoReputations(requests.map { it.ngoId }.toSet())
                },
                onFailure = { _state.update { it.copy(isRefreshing = false) } }
            )
        }
    }

    private fun fetchNgoReputations(ngoIds: Set<String>) {
        viewModelScope.launch {
            val reputations = ngoIds.map { id ->
                async {
                    try { id to organizationApi.getNgoReputation(id).data } catch (_: Exception) { null }
                }
            }.mapNotNull { it.await() }.toMap()
            _state.update { it.copy(ngoReputations = reputations) }
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allRequests

        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { request ->
                request.listingTitle.lowercase().contains(query) ||
                request.ngoName.lowercase().contains(query) ||
                request.notes?.lowercase()?.contains(query) == true
            }
        }

        current.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        current.filterCategory?.let { cat ->
            filtered = filtered.filter { it.listingCategory == cat }
        }

        current.filterPickupDatePreset?.let { preset ->
            val today = java.time.LocalDate.now()
            filtered = when (preset) {
                "today"     -> filtered.filter { it.pickupDate.take(10) == today.toString() }
                "this_week" -> filtered.filter {
                    runCatching {
                        val d = java.time.LocalDate.parse(it.pickupDate.take(10))
                        !d.isBefore(today) && !d.isAfter(today.plusDays(6))
                    }.getOrDefault(false)
                }
                "next_30"   -> filtered.filter {
                    runCatching {
                        val d = java.time.LocalDate.parse(it.pickupDate.take(10))
                        !d.isBefore(today) && !d.isAfter(today.plusDays(29))
                    }.getOrDefault(false)
                }
                else -> filtered
            }
        }

        filtered = when (current.selectedSort.value) {
            "date_desc"        -> filtered.sortedByDescending { it.createdAt }
            "date_asc"         -> filtered.sortedBy { it.createdAt }
            "pickup_date_asc"  -> filtered.sortedBy { it.pickupDate }
            "pickup_date_desc" -> filtered.sortedByDescending { it.pickupDate }
            else               -> filtered
        }

        _state.update { it.copy(filteredRequests = filtered) }
    }

    private fun approveRequest(requestId: String) {
        viewModelScope.launch {
            approvePickupRequestUseCase(requestId).fold(
                onSuccess = { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_approved))); loadRequests() },
                onFailure = { error -> _state.update { it.copy(error = error.message ?: "Failed to approve") } }
            )
        }
    }

    private fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            cancelPickupRequestUseCase(requestId).fold(
                onSuccess = { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_rejected))); loadRequests() },
                onFailure = { error -> _state.update { it.copy(error = error.message ?: "Failed to reject") } }
            )
        }
    }

    private fun markReadyForPickup(requestId: String) {
        viewModelScope.launch {
            markReadyForPickupUseCase(requestId).fold(
                onSuccess = { _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_marked_ready))); loadRequests() },
                onFailure = { error -> _state.update { it.copy(error = error.message ?: "Failed to mark as ready") } }
            )
        }
    }

    private fun bulkApprove() {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            try {
                val result = pickupRequestApi.bulkApprove(BulkActionRequest(ids))
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_requests_approved, result.succeeded)))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_bulk_approve_failed)))
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            loadRequests()
        }
    }

    private fun bulkReject(reason: String?) {
        val ids = _state.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isBulkOperating = true) }
            try {
                val result = pickupRequestApi.bulkReject(BulkRejectRequest(ids, reason))
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_n_requests_rejected, result.succeeded)))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_bulk_reject_failed)))
            }
            _state.update { it.copy(isBulkOperating = false, isSelectionMode = false, selectedIds = emptySet()) }
            loadRequests()
        }
    }
}
