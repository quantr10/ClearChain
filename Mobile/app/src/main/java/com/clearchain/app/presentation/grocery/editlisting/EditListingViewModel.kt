package com.clearchain.app.presentation.grocery.editlisting

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditListingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val listingRepository: ListingRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EditListingState())
    val state: StateFlow<EditListingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            getCurrentUserUseCase().first()?.let { user ->
                _state.update { it.copy(groceryHours = user.hours) }
            }
            listingRepository.getListingById(listingId).fold(
                onSuccess = { listing ->
                    _state.update {
                        it.copy(
                            listingId   = listing.id,
                            title       = listing.title,
                            description = listing.description,
                            category    = listing.category.name,
                            quantity    = listing.quantity.toString(),
                            unit        = listing.unit,
                            expiryDate  = listing.expiryDate.take(10),
                            imageUrl    = listing.imageUrl.orEmpty(),
                            isLoading   = false
                        )
                    }
                },
                onFailure = { e ->
                    val msg = e.message ?: context.getString(R.string.error_generic)
                    _state.update { it.copy(isLoading = false, error = msg) }
                    _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    fun onEvent(event: EditListingEvent) {
        when (event) {
            is EditListingEvent.TitleChanged       -> _state.update { it.copy(title = event.title, titleError = null) }
            is EditListingEvent.DescriptionChanged -> _state.update { it.copy(description = event.description, descriptionError = null) }
            is EditListingEvent.CategoryChanged    -> _state.update { it.copy(category = event.category, showCategoryDropdown = false) }
            is EditListingEvent.QuantityChanged    -> _state.update { it.copy(quantity = event.quantity.filter { c -> c.isDigit() }, quantityError = null) }
            is EditListingEvent.UnitChanged        -> _state.update { it.copy(unit = event.unit, showUnitDropdown = false, unitError = null) }
            is EditListingEvent.ExpiryDateChanged  -> _state.update { it.copy(expiryDate = event.date, expiryDateError = null) }
            is EditListingEvent.ToggleCategoryDropdown -> _state.update { it.copy(showCategoryDropdown = !it.showCategoryDropdown) }
            is EditListingEvent.ToggleUnitDropdown     -> _state.update { it.copy(showUnitDropdown = !it.showUnitDropdown) }
            is EditListingEvent.ClearError             -> _state.update { it.copy(error = null) }
            is EditListingEvent.SaveListing            -> saveListing()
        }
    }

    private fun saveListing() {
        if (!validate()) return
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            listingRepository.updateListing(
                id          = s.listingId,
                title       = s.title,
                description = s.description,
                category    = s.category,
                quantity    = s.quantity.toInt(),
                unit        = s.unit,
                expiryDate  = s.expiryDate,
                imageUrl    = s.imageUrl.ifBlank { null }
            ).fold(
                onSuccess = {
                    _state.update { it.copy(isSaving = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_updated)))
                    _uiEvent.send(UiEvent.NavigateUp)
                },
                onFailure = { e ->
                    val msg = e.message ?: context.getString(R.string.error_generic)
                    _state.update { it.copy(isSaving = false, error = msg) }
                    _uiEvent.send(UiEvent.ShowSnackbar(msg))
                }
            )
        }
    }

    private fun validate(): Boolean {
        val s = _state.value
        var valid = true
        if (s.title.isBlank()) {
            _state.update { it.copy(titleError = context.getString(R.string.error_title_required)) }
            valid = false
        } else if (s.title.length < 3) {
            _state.update { it.copy(titleError = context.getString(R.string.error_title_min_length)) }
            valid = false
        }
        if (s.description.isBlank()) {
            _state.update { it.copy(descriptionError = context.getString(R.string.error_description_required)) }
            valid = false
        }
        if (s.quantity.isBlank() || s.quantity.toIntOrNull() == null || s.quantity.toInt() <= 0) {
            _state.update { it.copy(quantityError = context.getString(R.string.error_quantity_required)) }
            valid = false
        }
        if (s.unit.isBlank()) {
            _state.update { it.copy(unitError = context.getString(R.string.error_unit_required)) }
            valid = false
        }
        if (s.expiryDate.isBlank()) {
            _state.update { it.copy(expiryDateError = context.getString(R.string.error_expiry_required)) }
            valid = false
        }
        return valid
    }
}
