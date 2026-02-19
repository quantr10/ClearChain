package com.clearchain.app.presentation.admin.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.AdminApi
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val adminApi: AdminApi
) : ViewModel() {

    private val _state = MutableStateFlow(TransactionsState())
    val state: StateFlow<TransactionsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadTransactions()
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