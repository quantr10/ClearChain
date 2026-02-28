package com.clearchain.app.presentation.grocery.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService  // ✅ ADD
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.domain.usecase.listing.DeleteListingUseCase
import com.clearchain.app.domain.usecase.listing.GetMyListingsUseCase
import com.clearchain.app.domain.usecase.listing.UpdateListingQuantityUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getMyListingsUseCase: GetMyListingsUseCase,
    private val deleteListingUseCase: DeleteListingUseCase,
    private val updateListingQuantityUseCase: UpdateListingQuantityUseCase,
    private val signalRService: SignalRService  // ✅ ADD
) : ViewModel() {

    private val _state = MutableStateFlow(MyListingsState())
    val state: StateFlow<MyListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadListings()
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
                        // Silent fail - app works without real-time
                    }
                    else -> {}
                }
            }
        }

        // ✅ Listen for listing created (confirmation when grocery creates)
        viewModelScope.launch {
            signalRService.listingCreated.collect { listing ->
                loadListings()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("✅ New listing created: ${listing.title}")
                )
            }
        }

        // ✅ Listen for listing deleted (confirmation)
        viewModelScope.launch {
            signalRService.listingDeleted.collect { notification ->
                loadListings()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Listing deleted")
                )
            }
        }

        // ✅ Listen for quantity changes (confirmation)
        viewModelScope.launch {
            signalRService.listingQuantityChanged.collect { notification ->
                loadListings()
                val change = if (notification.newQuantity > notification.oldQuantity) {
                    "increased"
                } else {
                    "decreased"
                }
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Quantity $change: ${notification.listing.title}")
                )
            }
        }

        // ✅ Listen for listing updates (general updates)
        viewModelScope.launch {
            signalRService.listingUpdated.collect { listing ->
                loadListings()
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Listing updated: ${listing.title}")
                )
            }
        }

        // ✅ CRITICAL: Listen for pickup requests (listings become RESERVED!)
        viewModelScope.launch {
            signalRService.pickupRequestCreated.collect { request ->
                loadListings() // Auto-refresh to show status change
                _uiEvent.send(
                    UiEvent.ShowSnackbar("📝 New pickup request for: ${request.listingTitle}")
                )
            }
        }

        // ✅ Listen for pickup request cancellations (listings become AVAILABLE again!)
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { request ->
                loadListings() // Auto-refresh
                _uiEvent.send(
                    UiEvent.ShowSnackbar("Request cancelled: ${request.listingTitle}")
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

    fun onEvent(event: MyListingsEvent) {
        when (event) {
            MyListingsEvent.LoadListings -> loadListings()
            MyListingsEvent.RefreshListings -> refreshListings()
            is MyListingsEvent.DeleteListing -> deleteListing(event.listingId)

            // Search & Sort
            is MyListingsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is MyListingsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }

            // Status Tab
            is MyListingsEvent.StatusTabChanged -> {
                _state.update { it.copy(selectedStatusTab = event.status) }
                applyFilters()
            }

            // Category Filter
            is MyListingsEvent.CategoryFilterChanged -> {
                _state.update { it.copy(selectedCategory = event.category) }
                applyFilters()
            }

            is MyListingsEvent.UpdateListingQuantity -> {
                updateListingQuantity(event.listingId, event.newQuantity)
            }

            MyListingsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getMyListingsUseCase()

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

            val result = getMyListingsUseCase()

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

    private fun deleteListing(listingId: String) {
        viewModelScope.launch {
            val result = deleteListingUseCase(listingId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Listing deleted"))
                    loadListings() // Reload after delete
                },
                onFailure = { error ->
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Failed to delete listing"))
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allListings

        // Apply STATUS TAB filter (first priority)
        current.selectedStatusTab?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        // Apply search
        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { listing ->
                listing.title.lowercase().contains(query) ||
                        listing.description.lowercase().contains(query) ||
                        listing.location.lowercase().contains(query) ||
                        listing.category.displayName().lowercase().contains(query)
            }
        }

        // Apply CATEGORY CHIP filter
        current.selectedCategory?.let { category ->
            filtered = filtered.filter { listing ->
                listing.category.name == category
            }
        }

        // Apply sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "name_asc" -> filtered.sortedBy { it.title }
            "name_desc" -> filtered.sortedByDescending { it.title }
            "quantity_desc" -> filtered.sortedByDescending { it.quantity }
            "quantity_asc" -> filtered.sortedBy { it.quantity }
            "expiry_asc" -> filtered.sortedBy { it.expiryDate }
            "expiry_desc" -> filtered.sortedByDescending { it.expiryDate }
            else -> filtered
        }

        _state.update { it.copy(filteredListings = filtered) }
    }

    private fun updateListingQuantity(listingId: String, newQuantity: Int) {
        viewModelScope.launch {
            val result = updateListingQuantityUseCase(listingId, newQuantity)
            
            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar("Quantity updated"))
                    loadListings() // Reload after update
                },
                onFailure = { error ->
                    _uiEvent.send(
                        UiEvent.ShowSnackbar(
                            error.message ?: "Failed to update quantity"
                        )
                    )
                }
            )
        }
    }
}