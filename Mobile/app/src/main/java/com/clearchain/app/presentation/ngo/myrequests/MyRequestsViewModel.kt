// ═══════════════════════════════════════════════════════════════════════════════
// MyRequestsViewModel.kt - COMPLETE WITH SEARCH, SORT, FILTER
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.myrequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
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
    private val confirmPickupUseCase: ConfirmPickupUseCase
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

            // NEW: Search, Sort, Filter handlers
            is MyRequestsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is MyRequestsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is MyRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is MyRequestsEvent.CancelRequest -> cancelRequest(event.requestId)
            is MyRequestsEvent.ConfirmPickup -> confirmPickup(event.requestId)

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

            val result = getMyPickupRequestsUseCase()

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

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW: Filter and Sort Logic
    // ═══════════════════════════════════════════════════════════════════════════

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allRequests

        // Apply search
        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { request ->
                request.listingTitle.lowercase().contains(query) ||
                        request.groceryName.lowercase().contains(query) ||
                        request.notes?.lowercase()?.contains(query) == true
            }
        }

        // Apply status filter
        current.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        // Apply sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "pickup_date_asc" -> filtered.sortedBy { it.pickupDate }
            "pickup_date_desc" -> filtered.sortedByDescending { it.pickupDate }
            else -> filtered
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

    private fun confirmPickup(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = confirmPickupUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Pickup confirmed!"))
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