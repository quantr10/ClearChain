package com.clearchain.app.presentation.grocery.createlisting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.domain.usecase.listing.CreateListingUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val createListingUseCase: CreateListingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: CreateListingEvent) {
        when (event) {
            is CreateListingEvent.TitleChanged -> {
                _state.update { it.copy(title = event.title, titleError = null) }
            }

            is CreateListingEvent.DescriptionChanged -> {
                _state.update { it.copy(description = event.description, descriptionError = null) }
            }

            is CreateListingEvent.CategoryChanged -> {
                _state.update {
                    it.copy(
                        category = event.category,
                        showCategoryDropdown = false
                    )
                }
            }

            is CreateListingEvent.QuantityChanged -> {
                // Only allow numbers
                val filtered = event.quantity.filter { it.isDigit() }
                _state.update { it.copy(quantity = filtered, quantityError = null) }
            }

            is CreateListingEvent.UnitChanged -> {
                _state.update {
                    it.copy(
                        unit = event.unit,
                        showUnitDropdown = false
                    )
                }
            }

            is CreateListingEvent.ExpiryDateChanged -> {
                _state.update { it.copy(expiryDate = event.date, expiryDateError = null) }
            }

            is CreateListingEvent.PickupTimeStartChanged -> {
                _state.update { it.copy(pickupTimeStart = event.time, pickupTimeStartError = null) }
            }

            is CreateListingEvent.PickupTimeEndChanged -> {
                _state.update { it.copy(pickupTimeEnd = event.time, pickupTimeEndError = null) }
            }

            is CreateListingEvent.ImageUrlChanged -> {
                _state.update { it.copy(imageUrl = event.url) }
            }

            CreateListingEvent.ToggleCategoryDropdown -> {
                _state.update { it.copy(showCategoryDropdown = !it.showCategoryDropdown) }
            }

            CreateListingEvent.ToggleUnitDropdown -> {
                _state.update { it.copy(showUnitDropdown = !it.showUnitDropdown) }
            }

            CreateListingEvent.CreateListing -> {
                createListing()
            }

            CreateListingEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }
        }
    }

    private fun createListing() {
        val currentState = _state.value

        // Validate
        if (!validateInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = createListingUseCase(
                title = currentState.title,
                description = currentState.description,
                category = currentState.category,
                quantity = currentState.quantity.toInt(),
                unit = currentState.unit,
                expiryDate = currentState.expiryDate,
                pickupTimeStart = currentState.pickupTimeStart,
                pickupTimeEnd = currentState.pickupTimeEnd,
                imageUrl = currentState.imageUrl.ifBlank { null }
            )

            result.fold(
                onSuccess = { listing ->
                    _state.update { it.copy(isLoading = false) }
                    _uiEvent.send(UiEvent.ShowSnackbar("Listing created successfully!"))
                    _uiEvent.send(UiEvent.NavigateUp)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to create listing"
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: "Failed to create listing"))
                }
            )
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        // Validate title
        if (currentState.title.isBlank()) {
            _state.update { it.copy(titleError = "Title is required") }
            isValid = false
        } else if (currentState.title.length < 3) {
            _state.update { it.copy(titleError = "Title must be at least 3 characters") }
            isValid = false
        }

        // Validate description
        if (currentState.description.isBlank()) {
            _state.update { it.copy(descriptionError = "Description is required") }
            isValid = false
        }

        // Validate quantity
        if (currentState.quantity.isBlank()) {
            _state.update { it.copy(quantityError = "Quantity is required") }
            isValid = false
        } else if (currentState.quantity.toIntOrNull() == null || currentState.quantity.toInt() <= 0) {
            _state.update { it.copy(quantityError = "Quantity must be greater than 0") }
            isValid = false
        }

        // Validate unit
        if (currentState.unit.isBlank()) {
            _state.update { it.copy(unitError = "Unit is required") }
            isValid = false
        }

        // Validate expiry date
        if (currentState.expiryDate.isBlank()) {
            _state.update { it.copy(expiryDateError = "Expiry date is required") }
            isValid = false
        }

        // Validate pickup times
        if (currentState.pickupTimeStart.isBlank()) {
            _state.update { it.copy(pickupTimeStartError = "Pickup start time is required") }
            isValid = false
        }

        if (currentState.pickupTimeEnd.isBlank()) {
            _state.update { it.copy(pickupTimeEndError = "Pickup end time is required") }
            isValid = false
        }

        return isValid
    }
}