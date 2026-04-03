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
    private val locationPreferenceStore: LocationPreferenceStore
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseListingsState(isCheckingLocation = true))
    val state: StateFlow<BrowseListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // First: check DataStore for saved preference, THEN decide if location is set
        viewModelScope.launch {
            val firstPref = locationPreferenceStore.locationPreference.first()

            if (firstPref != null) {
                _state.update {
                    it.copy(
                        userLat = firstPref.latitude,
                        userLng = firstPref.longitude,
                        radiusKm = firstPref.radiusKm,
                        locationDisplayName = firstPref.displayName,
                        isLocationSet = true,
                        isCheckingLocation = false
                    )
                }
                loadListings()
            } else {
                // No saved preference → let screen redirect to location picker
                _state.update { it.copy(isCheckingLocation = false, isLocationSet = false) }
            }
        }

        // Then: observe ongoing changes (when user returns from LocationPicker with new preference)
        viewModelScope.launch {
            locationPreferenceStore.locationPreference
                .drop(1)  // Skip the first emission (already handled above)
                .collect { pref ->
                    if (pref != null) {
                        val changed = _state.value.userLat != pref.latitude ||
                            _state.value.userLng != pref.longitude ||
                            _state.value.radiusKm != pref.radiusKm

                        _state.update {
                            it.copy(
                                userLat = pref.latitude,
                                userLng = pref.longitude,
                                radiusKm = pref.radiusKm,
                                locationDisplayName = pref.displayName,
                                isLocationSet = true
                            )
                        }

                        if (changed) loadListings()
                    }
                }
        }

        setupSignalR()
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }
        viewModelScope.launch {
            signalRService.connectionState.collect { s ->
                if (s is ConnectionState.Connected) _uiEvent.send(UiEvent.ShowSnackbar("Real-time updates enabled"))
            }
        }
        viewModelScope.launch {
            signalRService.listingCreated.collect {
                loadListings()
                _uiEvent.send(UiEvent.ShowSnackbar("New listing: ${it.title}"))
            }
        }
        viewModelScope.launch { signalRService.listingUpdated.collect { loadListings() } }
        viewModelScope.launch { signalRService.listingDeleted.collect { loadListings() } }
        viewModelScope.launch { signalRService.listingQuantityChanged.collect { loadListings() } }
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
            BrowseListingsEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun loadListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val s = _state.value
            val result = getAllListingsUseCase(
                status = "open",
                lat = s.userLat, lng = s.userLng,
                radiusKm = if (s.isLocationSet) s.radiusKm else null
            )
            result.fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isLoading = false) }
                    applyFilters()
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load") }
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
                lat = s.userLat, lng = s.userLng,
                radiusKm = if (s.isLocationSet) s.radiusKm else null
            )
            result.fold(
                onSuccess = { listings ->
                    _state.update { it.copy(allListings = listings, isRefreshing = false) }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar("Listings refreshed"))
                },
                onFailure = { e ->
                    _state.update { it.copy(isRefreshing = false, error = e.message) }
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allListings

        if (current.searchQuery.isNotBlank()) {
            val q = current.searchQuery.lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(q) ||
                    it.description.lowercase().contains(q) ||
                    it.groceryName.lowercase().contains(q) ||
                    it.location.lowercase().contains(q) ||
                    it.category.displayName().lowercase().contains(q)
            }
        }

        current.selectedCategory?.let { cat ->
            filtered = filtered.filter { it.category.name == cat }
        }

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