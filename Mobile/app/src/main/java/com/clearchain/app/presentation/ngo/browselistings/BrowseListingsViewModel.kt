package com.clearchain.app.presentation.ngo.browselistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.domain.usecase.listing.GetAllListingsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseListingsViewModel @Inject constructor(
    private val getAllListingsUseCase: GetAllListingsUseCase,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseListingsState())
    val state: StateFlow<BrowseListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadListings()
        setupSignalR()
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
                        // Silent fail - app works without real-time
                    }
                    else -> {}
                }
            }
        }

        // ✅ Listen for NEW listings
        viewModelScope.launch {
            signalRService.listingCreated.collect { listing ->
                loadListings() // Auto-refresh
                _uiEvent.send(
                    UiEvent.ShowSnackbar("📢 New listing: ${listing.title}")
                )
            }
        }

        // ✅ Listen for listing updates
        viewModelScope.launch {
            signalRService.listingUpdated.collect { listing ->
                loadListings()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Listing updated: ${listing.title}")
                )
            }
        }

        // ✅ Listen for listing deletions
        viewModelScope.launch {
            signalRService.listingDeleted.collect { notification ->
                loadListings()
                _uiEvent.send(UiEvent.ShowSnackbar("Listing removed"))
            }
        }

        // ✅ Listen for quantity changes
        viewModelScope.launch {
            signalRService.listingQuantityChanged.collect { notification ->
                loadListings()
                
                val quantityMsg = if (notification.newQuantity > notification.oldQuantity) {
                    "📈 More available: ${notification.listing.title}"
                } else {
                    "📉 Less available: ${notification.listing.title}"
                }
                
                _uiEvent.send(UiEvent.ShowSnackbar(quantityMsg))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: BrowseListingsEvent) {
        when (event) {
            BrowseListingsEvent.LoadListings -> loadListings()
            BrowseListingsEvent.RefreshListings -> refreshListings()

            is BrowseListingsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is BrowseListingsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is BrowseListingsEvent.CategoryFilterChanged -> {
                _state.update { it.copy(selectedCategory = event.category) }
                applyFilters()
            }

            is BrowseListingsEvent.NavigateToRequestPickup -> {
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.Navigate("request_pickup/${event.listingId}"))
                }
            }

            BrowseListingsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getAllListingsUseCase(status = "open")

            result.fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(
                            allListings = listings,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load listings"
                        )
                    }
                }
            )
        }
    }

    private fun refreshListings() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            val result = getAllListingsUseCase(status = "open")

            result.fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(
                            allListings = listings,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar("Listings refreshed"))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: "Failed to refresh listings"
                        )
                    }
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allListings

        // Apply search
        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { listing ->
                listing.title.lowercase().contains(query) ||
                        listing.description.lowercase().contains(query) ||
                        listing.groceryName.lowercase().contains(query) ||
                        listing.location.lowercase().contains(query) ||
                        listing.category.displayName().lowercase().contains(query)
            }
        }

        // Apply category filter
        current.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category.name == category }
        }

        // Apply sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "expiry_asc" -> filtered.sortedBy { it.expiryDate }
            "expiry_desc" -> filtered.sortedByDescending { it.expiryDate }
            "quantity_desc" -> filtered.sortedByDescending { it.quantity }
            "quantity_asc" -> filtered.sortedBy { it.quantity }
            "location_asc" -> filtered.sortedBy { it.location }
            "name_asc" -> filtered.sortedBy { it.title }
            "name_desc" -> filtered.sortedByDescending { it.title }
            else -> filtered
        }

        _state.update { it.copy(filteredListings = filtered) }
    }
}