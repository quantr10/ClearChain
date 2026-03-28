package com.clearchain.app.presentation.ngo.browselistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.local.LocationPreferenceStore
import com.clearchain.app.data.remote.signalr.ConnectionState
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.domain.usecase.listing.GetAllListingsUseCase
import com.clearchain.app.presentation.components.SortOption
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseListingsViewModel @Inject constructor(
    private val getAllListingsUseCase: GetAllListingsUseCase,
    private val signalRService: SignalRService,
    private val locationPreferenceStore: LocationPreferenceStore  // NEW (Part 2)
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseListingsState())
    val state: StateFlow<BrowseListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // Load location preference first, then load listings
        viewModelScope.launch {
            locationPreferenceStore.locationPreference.first()?.let { pref ->
                _state.update {
                    it.copy(
                        userLat = pref.latitude,
                        userLng = pref.longitude,
                        radiusKm = pref.radiusKm,
                        locationDisplayName = pref.displayName,
                        isLocationSet = true
                    )
                }
            }
            loadListings()
        }
        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }

        viewModelScope.launch {
            signalRService.connectionState.collect { state ->
                if (state is ConnectionState.Connected) {
                    _uiEvent.send(UiEvent.ShowSnackbar("Real-time updates enabled"))
                }
            }
        }

        viewModelScope.launch {
            signalRService.listingCreated.collect {
                loadListings()
                _uiEvent.send(UiEvent.ShowSnackbar("New listing: ${it.title}"))
            }
        }

        viewModelScope.launch {
            signalRService.listingUpdated.collect { loadListings() }
        }

        viewModelScope.launch {
            signalRService.listingDeleted.collect { loadListings() }
        }

        viewModelScope.launch {
            signalRService.listingQuantityChanged.collect { loadListings() }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { signalRService.disconnect() }
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

    // ═══ UPDATED: Pass location params to API (Part 2) ═══
    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value

            val result = getAllListingsUseCase(
                status = "open",
                lat = s.userLat,
                lng = s.userLng,
                radiusKm = if (s.isLocationSet) s.radiusKm else null
            )

            result.fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isLoading = false) }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to load listings")
                    }
                }
            )
        }
    }

    private fun refreshListings() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            val s = _state.value

            val result = getAllListingsUseCase(
                status = "open",
                lat = s.userLat,
                lng = s.userLng,
                radiusKm = if (s.isLocationSet) s.radiusKm else null
            )

            result.fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isRefreshing = false) }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar("Listings refreshed"))
                },
                onFailure = { error ->
                    _state.update { it.copy(isRefreshing = false, error = error.message) }
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allListings

        // Search
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

        // Category filter
        current.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category.name == category }
        }

        // Sort
        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "expiry_asc" -> filtered.sortedBy { it.expiryDate }
            "expiry_desc" -> filtered.sortedByDescending { it.expiryDate }
            "name_asc" -> filtered.sortedBy { it.title }
            "name_desc" -> filtered.sortedByDescending { it.title }
            else -> filtered
        }

        _state.update { it.copy(filteredListings = filtered) }
    }
}