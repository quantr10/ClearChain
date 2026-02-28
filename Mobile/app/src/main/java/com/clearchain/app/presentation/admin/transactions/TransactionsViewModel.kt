package com.clearchain.app.presentation.admin.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val adminApi: AdminApi,
    private val signalRService: SignalRService,  // ✅ ADD
    private val getCurrentUserUseCase: GetCurrentUserUseCase  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadTransactions()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        viewModelScope.launch {
            try {
                signalRService.connect()  // ✅ Use regular connect()
            } catch (e: Exception) {
                return@launch
            }

            // Listen for connection state
            launch {
                signalRService.connectionState.collect { state ->
                    when (state) {
                        is ConnectionState.Connected -> {
                            _uiEvent.send(UiEvent.ShowSnackbar("✅ Real-time monitoring enabled"))
                        }
                        else -> {}
                    }
                }
            }

            // ✅ Listen for new pickup requests
            launch {
                signalRService.pickupRequestCreated.collect { request ->
                    loadTransactions()
                    _uiEvent.send(
                        UiEvent.ShowSnackbar("📝 New pickup request from ${request.ngoName}")
                    )
                }
            }

            // ✅ Listen for status changes
            launch {
                signalRService.pickupRequestStatusChanged.collect { notification ->
                    loadTransactions()
                    _uiEvent.send(
                        UiEvent.ShowSnackbar("Status updated: ${notification.request.listingTitle}")
                    )
                }
            }

            // ✅ Listen for completed transactions
            launch {
                signalRService.transactionCompleted.collect { notification ->
                    loadTransactions()
                    _uiEvent.send(
                        UiEvent.ShowSnackbar("✅ Transaction completed: ${notification.productName}")
                    )
                }
            }

            // ✅ Listen for cancellations
            launch {
                signalRService.pickupRequestCancelled.collect { request ->
                    loadTransactions()
                    _uiEvent.send(
                        UiEvent.ShowSnackbar("Request cancelled: ${request.listingTitle}")
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: TransactionsEvent) {
        when (event) {
            TransactionsEvent.LoadTransactions -> loadTransactions()
            TransactionsEvent.RefreshTransactions -> refreshTransactions()

            is TransactionsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }

            is TransactionsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            TransactionsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val response = adminApi.getAllPickupRequests()
                val allRequests = response.data.map { it.toDomain() }
                    .sortedByDescending { it.createdAt }

                _state.update {
                    it.copy(
                        allTransactions = allRequests,
                        filteredTransactions = allRequests,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to load transactions",
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun refreshTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            try {
                val response = adminApi.getAllPickupRequests()
                val allRequests = response.data.map { it.toDomain() }
                    .sortedByDescending { it.createdAt }

                _state.update {
                    it.copy(
                        allTransactions = allRequests,
                        isRefreshing = false
                    )
                }
                applyFilters()
                _uiEvent.send(UiEvent.ShowSnackbar("Transactions refreshed"))
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "Failed to refresh transactions",
                        isRefreshing = false
                    )
                }
            }
        }
    }

    private fun applyFilters() {
        val currentState = _state.value
        var filtered = currentState.allTransactions

        // Filter by status
        currentState.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        // Filter by search query
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { transaction ->
                transaction.listingTitle.lowercase().contains(query) ||
                        transaction.groceryName.lowercase().contains(query) ||
                        transaction.ngoName.lowercase().contains(query) ||
                        transaction.listingCategory.lowercase().contains(query)
            }
        }

        _state.update { it.copy(filteredTransactions = filtered) }
    }
}