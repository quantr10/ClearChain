package com.clearchain.app.presentation.ngo.myrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase  // ✅ NEW
import com.clearchain.app.domain.usecase.pickuprequest.GetMyPickupRequestsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    private val getMyPickupRequestsUseCase: GetMyPickupRequestsUseCase,
    private val cancelPickupRequestUseCase: CancelPickupRequestUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase  // ✅ NEW
) : ViewModel() {

    private val _state = MutableStateFlow(MyRequestsState())
    val state: StateFlow<MyRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadRequests()
    }

    fun onEvent(event: MyRequestsEvent) {
        when (event) {
            MyRequestsEvent.LoadRequests -> loadRequests()
            MyRequestsEvent.RefreshRequests -> refreshRequests()

            is MyRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is MyRequestsEvent.CancelRequest -> cancelRequest(event.requestId)
            is MyRequestsEvent.ConfirmPickup -> confirmPickup(event.requestId)  // ✅ NEW

            MyRequestsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getMyPickupRequestsUseCase()

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

            val result = getMyPickupRequestsUseCase()

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

    private fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = cancelPickupRequestUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Request cancelled successfully"))
                    loadRequests() // Reload list
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to cancel request",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    // ✅ NEW METHOD
    private fun confirmPickup(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = confirmPickupUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Pickup confirmed! Thank you for reducing food waste."))
                    loadRequests() // Reload list
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Failed to confirm pickup",
                            isLoading = false
                        )
                    }
                }
            )
        }
    }
}