package com.clearchain.app.presentation.grocery.createlisting

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.R
import com.clearchain.app.data.local.ListingDraft
import com.clearchain.app.data.local.ListingDraftStore
import com.clearchain.app.data.remote.dto.FoodAnalysisData
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.listing.CreateListingUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateListingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val createListingUseCase: CreateListingUseCase,
    private val listingRepository: ListingRepository,
    private val draftStore: ListingDraftStore,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        startAutoSave()
        loadGroceryHours()
    }

    private fun loadGroceryHours() {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { org ->
                _state.update { it.copy(groceryHours = org.hours) }
            }
        }
    }

    private fun startAutoSave() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                saveDraft()
            }
        }
    }

    private suspend fun saveDraft() {
        val s = _state.value
        if (s.title.isBlank() && s.description.isBlank() && s.quantity.isBlank()) return
        val draft = ListingDraft(
            title = s.title,
            description = s.description,
            category = s.category,
            quantity = s.quantity,
            unit = s.unit,
            expiryDate = s.expiryDate
        )
        draftStore.save(draft)
        _state.update { it.copy(draftSavedAt = System.currentTimeMillis()) }
    }

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

            is CreateListingEvent.AddImage -> {
                val current = _state.value.selectedImages
                if (current.size < 5) {
                    val updated = current + event.uri
                    _state.update {
                        it.copy(
                            selectedImages = updated,
                            selectedImageUri = updated.first(),
                            showImagePicker = false
                        )
                    }
                    if (current.isEmpty()) analyzeImage()
                }
            }

            is CreateListingEvent.RemoveImage -> {
                val updated = _state.value.selectedImages.toMutableList()
                    .also { it.removeAt(event.index) }
                _state.update {
                    it.copy(
                        selectedImages = updated,
                        selectedImageUri = updated.firstOrNull()
                    )
                }
            }

            is CreateListingEvent.ReorderImages -> {
                val list = _state.value.selectedImages.toMutableList()
                val item = list.removeAt(event.fromIndex)
                list.add(event.toIndex, item)
                _state.update {
                    it.copy(
                        selectedImages = list,
                        selectedImageUri = list.firstOrNull()
                    )
                }
            }

            CreateListingEvent.TogglePreview -> {
                _state.update { it.copy(isPreviewMode = !it.isPreviewMode) }
            }

            CreateListingEvent.RestoreDraft -> {
                viewModelScope.launch {
                    draftStore.draft.first()?.let { draft ->
                        _state.update {
                            it.copy(
                                title = draft.title,
                                description = draft.description,
                                category = draft.category,
                                quantity = draft.quantity,
                                unit = draft.unit,
                                expiryDate = draft.expiryDate,
                                draftSavedAt = draft.savedAt
                            )
                        }
                        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_draft_restored)))
                    }
                }
            }

            CreateListingEvent.ClearDraft -> {
                viewModelScope.launch { draftStore.clear() }
                _state.update { it.copy(draftSavedAt = null) }
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
        
        val primaryUri = currentState.selectedImages.firstOrNull() ?: currentState.selectedImageUri
        if (primaryUri != null && finalImageUrl.isEmpty()) {
            _state.update { it.copy(isLoading = true, error = context.getString(R.string.uploading_image)) }

            val uploadResult = listingRepository.uploadFoodImage(primaryUri)
            
            uploadResult.fold(
                onSuccess = { uploadedUrl ->
                    finalImageUrl = uploadedUrl
                    Log.d("CreateListingVM", "✅ Image uploaded: $uploadedUrl")
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = context.getString(R.string.snack_image_upload_failed, error.message ?: "")
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_image_upload_failed, error.message ?: "")))
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

                // Clear draft on success
                draftStore.clear()

                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_listing_created)))
                _uiEvent.send(UiEvent.NavigateUp)
            },
            onFailure = { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.message ?: context.getString(R.string.error_create_listing_failed)
                    )
                }
                _uiEvent.send(UiEvent.ShowSnackbar(error.message ?: context.getString(R.string.error_create_listing_failed)))
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
            _state.update { it.copy(titleError = context.getString(R.string.error_title_required)) }
            isValid = false
        } else if (currentState.title.length < 3) {
            _state.update { it.copy(titleError = context.getString(R.string.error_title_min_length)) }
            isValid = false
        }

        if (currentState.description.isBlank()) {
            _state.update { it.copy(descriptionError = context.getString(R.string.error_description_required)) }
            isValid = false
        }

        if (currentState.quantity.isBlank()) {
            _state.update { it.copy(quantityError = context.getString(R.string.error_quantity_required)) }
            isValid = false
        } else if (currentState.quantity.toIntOrNull() == null || currentState.quantity.toInt() <= 0) {
            _state.update { it.copy(quantityError = context.getString(R.string.error_quantity_positive)) }
            isValid = false
        }

        if (currentState.unit.isBlank()) {
            _state.update { it.copy(unitError = context.getString(R.string.error_unit_required)) }
            isValid = false
        }

        if (currentState.expiryDate.isBlank()) {
            _state.update { it.copy(expiryDateError = context.getString(R.string.error_expiry_required)) }
            isValid = false
        } else if (!isValidDateFormat(currentState.expiryDate)) {
            _state.update { it.copy(expiryDateError = context.getString(R.string.error_date_format_yyyy_mm_dd)) }
            isValid = false
        }

        return isValid
    }

    private fun isValidDateFormat(date: String): Boolean {
        val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")
        return dateRegex.matches(date)
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
                    _uiEvent.send(UiEvent.ShowSnackbar(
                        context.getString(R.string.snack_ai_detected, analysisData.title, (analysisData.confidence * 100).toInt())
                    ))
                },
                onFailure = { error ->
                    _state.update { 
                        it.copy(
                            isAnalyzing = false,
                            analysisError = error.message ?: context.getString(R.string.error_analysis_failed)
                        ) 
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_analysis_failed, error.message ?: "")))
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
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_ai_applied)))
        }
    }
}