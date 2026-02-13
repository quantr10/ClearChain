package com.clearchain.app.presentation.ngo.browselistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.domain.usecase.listing.GetAllListingsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseListingsViewModel @Inject constructor(
    private val getAllListingsUseCase: GetAllListingsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseListingsState())
    val state: StateFlow<BrowseListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadListings()
    }

    fun onEvent(event: BrowseListingsEvent) {
        when (event) {
            BrowseListingsEvent.LoadListings -> {
                loadListings()
            }

            BrowseListingsEvent.RefreshListings -> {
                refreshListings()
            }

            is BrowseListingsEvent.SearchQueryChanged -> {
                _state.update {
                    it.copy(searchQuery = event.query)
                }
                applyFilters()
            }

            is BrowseListingsEvent.CategoryFilterChanged -> {
                _state.update {
                    it.copy(selectedCategory = event.category)
                }
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

            val result = getAllListingsUseCase(status = "available")

            result.fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(
                            listings = listings.sortedByDescending { listing -> listing.createdAt },
                            filteredListings = listings.sortedByDescending { listing -> listing.createdAt },
                            isLoading = false
                        )
                    }
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

            val result = getAllListingsUseCase(status = "available")

            result.fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(
                            listings = listings.sortedByDescending { listing -> listing.createdAt },
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
        val currentState = _state.value
        var filtered = currentState.listings

        // Apply category filter
        currentState.selectedCategory?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        // Apply search query
        if (currentState.searchQuery.isNotBlank()) {
            val query = currentState.searchQuery.lowercase()
            filtered = filtered.filter { listing ->
                listing.title.lowercase().contains(query) ||
                        listing.description.lowercase().contains(query) ||
                        listing.groceryName.lowercase().contains(query) ||
                        listing.location.lowercase().contains(query)
            }
        }

        _state.update { it.copy(filteredListings = filtered) }
    }
}