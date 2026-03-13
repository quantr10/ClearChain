package com.clearchain.app.presentation.grocery.createlisting

import android.net.Uri
import android.util.Log  // ✅ ADD THIS
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.dto.FoodAnalysisData  // ✅ ADD THIS
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.listing.CreateListingUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateListingViewModel @Inject constructor(
    private val createListingUseCase: CreateListingUseCase,
    private val listingRepository: ListingRepository
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
                val filtered = event.quantity.filter { it.isDigit() }
                _state.update { it.copy(quantity = filtered, quantityError = null) }
            }

            is CreateListingEvent.UnitChanged -> {
                _state.update {
                    it.copy(
                        unit = event.unit,
                        showUnitDropdown = false,
                        unitError = null
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

            is CreateListingEvent.ImageSelected -> {
                _state.update { 
                    it.copy(
                        selectedImageUri = event.uri,
                        showImagePicker = false
                    ) 
                }
                analyzeImage()
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

            CreateListingEvent.AnalyzeImage -> {
                analyzeImage()
            }
            
            CreateListingEvent.ApplyAISuggestions -> {
                applyAISuggestions()
            }
            
            CreateListingEvent.ToggleImagePicker -> {
                _state.update { it.copy(showImagePicker = !it.showImagePicker) }
            }
            
            CreateListingEvent.ClearImage -> {
                _state.update { 
                    it.copy(
                        selectedImageUri = null,
                        analysisResult = null,
                        analysisError = null,
                        imageUrl = ""
                    ) 
                }
            }
        }
    }

private fun createListing() {
    val currentState = _state.value

    if (!validateInputs()) {
        return
    }

    viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        // ✅ NEW: Upload image first if user selected one
        var finalImageUrl = currentState.imageUrl
        
        if (currentState.selectedImageUri != null && finalImageUrl.isEmpty()) {
            _state.update { it.copy(isLoading = true, error = "Uploading image...") }
            
            val uploadResult = listingRepository.uploadFoodImage(currentState.selectedImageUri!!)
            
            uploadResult.fold(
                onSuccess = { uploadedUrl ->
                    finalImageUrl = uploadedUrl
                    Log.d("CreateListingVM", "✅ Image uploaded: $uploadedUrl")
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Image upload failed: ${error.message}"
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar("Image upload failed: ${error.message}"))
                    return@launch // Stop if upload fails
                }
            )
        }

        // Create listing with uploaded imageUrl
        val result = createListingUseCase(
            title = currentState.title,
            description = currentState.description,
            category = currentState.category,
            quantity = currentState.quantity.toInt(),
            unit = currentState.unit,
            expiryDate = currentState.expiryDate,
            pickupTimeStart = currentState.pickupTimeStart,
            pickupTimeEnd = currentState.pickupTimeEnd,
            imageUrl = finalImageUrl.ifBlank { null }
        )

        result.fold(
            onSuccess = { listing ->
                _state.update { it.copy(isLoading = false) }
                
                // Save analysis to DB if AI was used
                if (currentState.analysisResult != null) {
                    // Update imageUrl in analysis before saving
                    val updatedAnalysis = currentState.analysisResult!!.copy(
                        imageUrl = finalImageUrl
                    )
                    saveAnalysisToDatabase(updatedAnalysis)
                }
                
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

// ✅ NEW: Save analysis after listing created
private fun saveAnalysisToDatabase(analysisData: FoodAnalysisData) {
        viewModelScope.launch {
            try {
                listingRepository.saveAnalysis(analysisData)
                Log.d("CreateListingVM", "✅ Analysis saved to database")
            } catch (e: Exception) {
                // Don't fail - analysis save is non-critical
                Log.e("CreateListingVM", "⚠️ Failed to save analysis (non-critical): ${e.message}")
            }
        }
    }

    private fun validateInputs(): Boolean {
        val currentState = _state.value
        var isValid = true

        if (currentState.title.isBlank()) {
            _state.update { it.copy(titleError = "Title is required") }
            isValid = false
        } else if (currentState.title.length < 3) {
            _state.update { it.copy(titleError = "Title must be at least 3 characters") }
            isValid = false
        }

        if (currentState.description.isBlank()) {
            _state.update { it.copy(descriptionError = "Description is required") }
            isValid = false
        }

        if (currentState.quantity.isBlank()) {
            _state.update { it.copy(quantityError = "Quantity is required") }
            isValid = false
        } else if (currentState.quantity.toIntOrNull() == null || currentState.quantity.toInt() <= 0) {
            _state.update { it.copy(quantityError = "Quantity must be greater than 0") }
            isValid = false
        }

        if (currentState.unit.isBlank()) {
            _state.update { it.copy(unitError = "Unit is required") }
            isValid = false
        }

        if (currentState.expiryDate.isBlank()) {
            _state.update { it.copy(expiryDateError = "Expiry date is required") }
            isValid = false
        } else if (!isValidDateFormat(currentState.expiryDate)) {
            _state.update { it.copy(expiryDateError = "Invalid date format. Use YYYY-MM-DD") }
            isValid = false
        }

        if (currentState.pickupTimeStart.isBlank()) {
            _state.update { it.copy(pickupTimeStartError = "Pickup start time is required") }
            isValid = false
        } else if (!isValidTimeFormat(currentState.pickupTimeStart)) {
            _state.update { it.copy(pickupTimeStartError = "Invalid time format. Use HH:MM (e.g., 09:00)") }
            isValid = false
        }

        if (currentState.pickupTimeEnd.isBlank()) {
            _state.update { it.copy(pickupTimeEndError = "Pickup end time is required") }
            isValid = false
        } else if (!isValidTimeFormat(currentState.pickupTimeEnd)) {
            _state.update { it.copy(pickupTimeEndError = "Invalid time format. Use HH:MM (e.g., 17:00)") }
            isValid = false
        }

        if (isValid && currentState.pickupTimeStart.isNotBlank() && currentState.pickupTimeEnd.isNotBlank()) {
            if (!isEndTimeAfterStartTime(currentState.pickupTimeStart, currentState.pickupTimeEnd)) {
                _state.update { it.copy(pickupTimeEndError = "End time must be after start time") }
                isValid = false
            }
        }

        return isValid
    }

    private fun isValidTimeFormat(time: String): Boolean {
        val timeRegex = Regex("^([0-1][0-9]|2[0-3]):[0-5][0-9]$")
        return timeRegex.matches(time)
    }

    private fun isValidDateFormat(date: String): Boolean {
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        return dateRegex.matches(date)
    }

    private fun isEndTimeAfterStartTime(startTime: String, endTime: String): Boolean {
        return try {
            val start = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val end = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            end > start
        } catch (e: Exception) {
            false
        }
    }

    private fun analyzeImage() {
        val imageUri = _state.value.selectedImageUri ?: return
        
        viewModelScope.launch {
            _state.update { it.copy(isAnalyzing = true, analysisError = null) }
            
            val result = listingRepository.analyzeImage(imageUri)
            
            result.fold(
                onSuccess = { analysisData ->
                    _state.update { 
                        it.copy(
                            isAnalyzing = false,
                            analysisResult = analysisData,
                            imageUrl = analysisData.imageUrl
                        ) 
                    }
                    _uiEvent.send(
                        UiEvent.ShowSnackbar(
                            "AI detected: ${analysisData.title} (${(analysisData.confidence * 100).toInt()}% confidence)"
                        )
                    )
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isAnalyzing = false,
                            analysisError = error.message ?: "Analysis failed"
                        ) 
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar("Analysis failed: ${error.message}"))
                }
            )
        }
    }
    
    private fun applyAISuggestions() {
        val analysis = _state.value.analysisResult ?: return
        
        _state.update { 
            it.copy(
                title = analysis.title,
                description = analysis.notes,
                category = analysis.category.uppercase(),
                expiryDate = analysis.expiryDate.substring(0, 10),
                imageUrl = analysis.imageUrl
            ) 
        }
        
        viewModelScope.launch {
            _uiEvent.send(UiEvent.ShowSnackbar("AI suggestions applied! Please review and adjust."))
        }
    }
}