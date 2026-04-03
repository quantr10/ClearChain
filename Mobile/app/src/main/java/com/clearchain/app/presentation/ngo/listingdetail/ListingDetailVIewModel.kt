package com.clearchain.app.presentation.ngo.listingdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.repository.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ═══ ViewModel ═══
@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ListingDetailState())
    val state: StateFlow<ListingDetailState> = _state.asStateFlow()

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = listingRepository.getListingById(listingId)
            result.fold(
                onSuccess = { listing ->
                    _state.update { it.copy(listing = listing, isLoading = false) }
                },
                onFailure = { e ->
                    _state.update { it.copy(error = e.message ?: "Failed to load listing", isLoading = false) }
                }
            )
        }
    }
}