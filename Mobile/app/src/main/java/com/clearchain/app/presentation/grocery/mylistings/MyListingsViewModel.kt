package com.clearchain.app.presentation.grocery.mylistings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.listing.DeleteListingUseCase
import com.clearchain.app.domain.usecase.listing.GetMyListingsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel @Inject constructor(
    private val getMyListingsUseCase: GetMyListingsUseCase,
    private val deleteListingUseCase: DeleteListingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MyListingsState())
    val state: StateFlow<MyListingsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        loadListings()
    }

    fun onEvent(event: MyListingsEvent) {
        when (event) {
            MyListingsEvent.LoadListings -> {
                loadListings()
            }

            MyListingsEvent.RefreshListings -> {
                refreshListings()
            }

            is MyListingsEvent.DeleteListing -> {
                deleteListing(event.listingId)
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
                            listings = listings.sortedByDescending { listing -> listing.createdAt },
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

            val result = getMyListingsUseCase()

            result.fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(
                            listings = listings.sortedByDescending { listing -> listing.createdAt },
                            isRefreshing = false
                        )
                    }
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
}