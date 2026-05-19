package com.clearchain.app.presentation.ngo.browselistings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.LocationPreferenceStore
import com.clearchain.app.data.remote.api.SavedListingApi
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.model.displayName
import com.clearchain.app.domain.usecase.listing.GetAllListingsUseCase
import com.clearchain.app.presentation.components.SortOption
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseListingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAllListingsUseCase: GetAllListingsUseCase,
    private val signalRService: SignalRService,
    private val locationPreferenceStore: LocationPreferenceStore,
    private val savedListingApi: SavedListingApi
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

        loadSavedIds()
        setupSignalR()
    }

    private fun loadSavedIds() {
        viewModelScope.launch {
            try {
                val response = savedListingApi.getSavedListingIds()
                _state.update { it.copy(favoritedIds = response.data.toSet()) }
            } catch (_: Exception) {
                // Non-fatal — favorites show as empty
            }
        }
    }

    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }
        viewModelScope.launch {
            signalRService.listingCreated.collect {
                refreshBoth()
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_new_listing, it.title)))
            }
        }
        viewModelScope.launch { signalRService.listingUpdated.collect { refreshBoth() } }
        viewModelScope.launch { signalRService.listingDeleted.collect { refreshBoth() } }
        viewModelScope.launch { signalRService.listingQuantityChanged.collect { refreshBoth() } }
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
                    _uiEvent.send(UiEvent.Navigate(Screen.RequestPickup.createRoute(event.listingId)))
                }
            }
            BrowseListingsEvent.ClearError -> _state.update { it.copy(error = null) }

            // Favorites
            is BrowseListingsEvent.ToggleFavorite -> toggleFavorite(event.listingId)
            BrowseListingsEvent.ToggleFavoritesOnly -> {
                _state.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
                applyFilters()
            }

            // Advanced filters
            BrowseListingsEvent.ShowFilterSheet -> _state.update { it.copy(showFilterSheet = true) }
            BrowseListingsEvent.HideFilterSheet -> _state.update { it.copy(showFilterSheet = false) }
            is BrowseListingsEvent.FilterMinQuantityChanged -> {
                _state.update { it.copy(filterMinQuantity = event.min) }
                applyFilters()
            }
            is BrowseListingsEvent.FilterMaxQuantityChanged -> {
                _state.update { it.copy(filterMaxQuantity = event.max) }
                applyFilters()
            }
            is BrowseListingsEvent.FilterMinExpiryDaysChanged -> {
                _state.update { it.copy(filterMinExpiryDays = event.days) }
                applyFilters()
            }
            is BrowseListingsEvent.FilterMaxExpiryDaysChanged -> {
                _state.update { it.copy(filterMaxExpiryDays = event.days) }
                applyFilters()
            }
            is BrowseListingsEvent.FilterMaxDistanceChanged -> {
                _state.update { it.copy(filterMaxDistanceKm = event.km) }
                applyFilters()
            }
            BrowseListingsEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(
                    selectedCategory = null,
                    filterMinQuantity = 0,
                    filterMaxQuantity = null,
                    filterMinExpiryDays = 0,
                    filterMaxExpiryDays = null,
                    filterMaxDistanceKm = null,
                    showFavoritesOnly = false
                ) }
                applyFilters()
            }

            BrowseListingsEvent.ToggleMapView -> {
                val toMap = !_state.value.showMapView
                _state.update { it.copy(showMapView = toMap) }
                if (toMap) loadAllMapListings()
            }
            is BrowseListingsEvent.GroceryPinTapped ->
                _state.update { it.copy(selectedGroceryKey = event.key) }
            BrowseListingsEvent.DismissGrocerySheet ->
                _state.update { it.copy(selectedGroceryKey = null) }
        }
    }

    private fun refreshBoth() {
        loadListings()
        if (_state.value.showMapView) loadAllMapListings()
    }

    private fun loadAllMapListings() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMapListings = true) }
            getAllListingsUseCase(status = "open", lat = null, lng = null, radiusKm = null)
                .fold(
                    onSuccess = { listings ->
                        _state.update { it.copy(allMapListings = listings, isLoadingMapListings = false) }
                        applyFilters()
                    },
                    onFailure = { _state.update { it.copy(isLoadingMapListings = false) } }
                )
        }
    }

    private fun toggleFavorite(listingId: String) {
        val wasSaved = listingId in _state.value.favoritedIds
        // Optimistic update
        _state.update {
            val updated = if (wasSaved) it.favoritedIds - listingId else it.favoritedIds + listingId
            it.copy(favoritedIds = updated)
        }
        applyFilters()
        viewModelScope.launch {
            try {
                if (wasSaved) savedListingApi.unsaveListing(listingId)
                else savedListingApi.saveListing(listingId)
            } catch (_: Exception) {
                // Revert on failure
                _state.update {
                    val reverted = if (wasSaved) it.favoritedIds + listingId else it.favoritedIds - listingId
                    it.copy(favoritedIds = reverted)
                }
                applyFilters()
            }
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
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listings_refreshed)))
                },
                onFailure = { e ->
                    _state.update { it.copy(isRefreshing = false, error = e.message) }
                }
            )
        }
    }

    private fun applyFilters() {
        val s = _state.value

        val listFiltered = applyCommonFilters(s.allListings, s).let { result ->
            s.filterMaxDistanceKm?.let { maxKm ->
                result.filter { it.distanceKm == null || it.distanceKm <= maxKm }
            } ?: result
        }.applySorting(s)

        val mapFiltered = applyCommonFilters(s.allMapListings, s).applySorting(s)

        _state.update { it.copy(filteredListings = listFiltered, mapFilteredListings = mapFiltered) }
    }

    private fun applyCommonFilters(listings: List<Listing>, s: BrowseListingsState): List<Listing> {
        var result = listings
        if (s.searchQuery.isNotBlank()) {
            val q = s.searchQuery.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.groceryName.lowercase().contains(q) ||
                it.location.lowercase().contains(q) ||
                it.category.displayName().lowercase().contains(q)
            }
        }
        s.selectedCategory?.let { cat -> result = result.filter { it.category.name == cat } }
        if (s.showFavoritesOnly) result = result.filter { it.id in s.favoritedIds }
        if (s.filterMinQuantity > 0) result = result.filter { it.quantity >= s.filterMinQuantity }
        s.filterMaxQuantity?.let { max -> result = result.filter { it.quantity <= max } }
        if (s.filterMinExpiryDays > 0) {
            val today = java.time.LocalDate.now()
            result = result.filter { listing ->
                runCatching {
                    val expiry = java.time.LocalDate.parse(listing.expiryDate.take(10))
                    java.time.temporal.ChronoUnit.DAYS.between(today, expiry) >= s.filterMinExpiryDays
                }.getOrDefault(true)
            }
        }
        s.filterMaxExpiryDays?.let { maxDays ->
            val today = java.time.LocalDate.now()
            result = result.filter { listing ->
                runCatching {
                    val expiry = java.time.LocalDate.parse(listing.expiryDate.take(10))
                    java.time.temporal.ChronoUnit.DAYS.between(today, expiry) in 0..maxDays
                }.getOrDefault(true)
            }
        }
        return result
    }

    private fun List<Listing>.applySorting(s: BrowseListingsState): List<Listing> = when (s.selectedSort.value) {
        "date_desc"  -> sortedByDescending { it.createdAt }
        "date_asc"   -> sortedBy { it.createdAt }
        "expiry_asc" -> sortedBy { it.expiryDate }
        "expiry_desc"-> sortedByDescending { it.expiryDate }
        "name_asc"   -> sortedBy { it.title }
        "name_desc"  -> sortedByDescending { it.title }
        else         -> this
    }
}