package com.clearchain.app.presentation.grocery.managerequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.usecase.pickuprequest.ApprovePickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.GetGroceryPickupRequestsUseCase
import com.clearchain.app.domain.usecase.pickuprequest.MarkReadyForPickupUseCase
import com.clearchain.app.domain.usecase.pickuprequest.RejectPickupRequestUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageRequestsViewModel @Inject constructor(
    private val getGroceryPickupRequestsUseCase: GetGroceryPickupRequestsUseCase,
    private val approvePickupRequestUseCase: ApprovePickupRequestUseCase,
    private val rejectPickupRequestUseCase: RejectPickupRequestUseCase,
    private val markReadyForPickupUseCase: MarkReadyForPickupUseCase,
    private val signalRService: SignalRService  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(ManageRequestsState())
    val state: StateFlow<ManageRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadRequests()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        // Connect to SignalR (reuse existing connection)
        viewModelScope.launch {
            signalRService.connect()
        }

        // Listen for connection state
        viewModelScope.launch {
            signalRService.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time updates enabled"))
                    }
                    is ConnectionState.Error -> {
                        // Silent fail
                    }
                    else -> {}
                }
            }
        }

        // ✅ Listen for NEW pickup requests (important for groceries!)
        viewModelScope.launch {
            signalRService.pickupRequestCreated.collect { request ->
                loadRequests()
                _uiEvent.send(
                    UiEvent.ShowSnackbar(
                        "📢 New pickup request from ${request.ngoName}"
                    )
                )
            }
        }

        // ✅ Listen for status changes
        viewModelScope.launch {
            signalRService.pickupRequestStatusChanged.collect { notification ->
                loadRequests()
                
                val statusMessage = when (notification.newStatus.lowercase()) {
                    "completed" -> "✅ ${notification.request.ngoName} confirmed pickup"
                    "cancelled" -> "❌ Request cancelled by NGO"
                    else -> "Status updated to ${notification.newStatus}"
                }
                
                _uiEvent.send(UiEvent.ShowSnackbar(statusMessage))
            }
        }

        // ✅ Listen for cancellations
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { request ->
                loadRequests()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Request cancelled by ${request.ngoName}")
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
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

            is ManageRequestsEvent.ApproveRequest -> approveRequest(event.requestId)
            is ManageRequestsEvent.RejectRequest -> rejectRequest(event.requestId)
            is ManageRequestsEvent.MarkReady -> markReadyForPickup(event.requestId)

            ManageRequestsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getGroceryPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load requests"
                        )
                    }
                }
            )
        }
    }

    private fun refreshRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            val result = getGroceryPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar("Requests refreshed"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Failed to refresh requests"
                        )
                    }
                }
            )
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

        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "pickup_date_asc" -> filtered.sortedBy { it.pickupDate }
            "pickup_date_desc" -> filtered.sortedByDescending { it.pickupDate }
            else -> filtered
        }

        _state.update { it.copy(filteredRequests = filtered) }
    }

    private fun approveRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = approvePickupRequestUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Request approved successfully"))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to approve request",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = rejectPickupRequestUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Request rejected"))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to reject request",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun markReadyForPickup(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = markReadyForPickupUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Marked as ready for pickup"))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to mark as ready",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
}