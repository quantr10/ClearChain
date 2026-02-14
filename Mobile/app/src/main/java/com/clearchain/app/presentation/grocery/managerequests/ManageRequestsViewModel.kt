package com.clearchain.app.presentation.grocery.managerequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.pickuprequest.*
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
    private val markPickedUpUseCase: MarkPickedUpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ManageRequestsState())
    val state: StateFlow<ManageRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadRequests()
    }

    fun onEvent(event: ManageRequestsEvent) {
        when (event) {
            ManageRequestsEvent.LoadRequests -> loadRequests()
            ManageRequestsEvent.RefreshRequests -> refreshRequests()

            is ManageRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is ManageRequestsEvent.ApproveRequest -> approveRequest(event.requestId)
            is ManageRequestsEvent.RejectRequest -> rejectRequest(event.requestId)
            is ManageRequestsEvent.MarkReady -> markReady(event.requestId)
            is ManageRequestsEvent.MarkPickedUp -> markPickedUp(event.requestId)

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
                            requests = requests.sortedByDescending { req -> req.createdAt },
                            filteredRequests = requests.sortedByDescending { req -> req.createdAt },
                            isLoading = false
                        )
                    }
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
                            requests = requests.sortedByDescending { req -> req.createdAt },
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
        val currentState = _state.value
        var filtered = currentState.requests

        currentState.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
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

    private fun markReady(requestId: String) {
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

    private fun markPickedUp(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = markPickedUpUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Pickup completed!"))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to mark as picked up",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
}