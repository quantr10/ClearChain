package com.clearchain.app.presentation.grocery.createlisting

import android.net.Uri
import com.clearchain.app.data.remote.dto.FoodAnalysisData

data class CreateListingState(
    val title: String = "",
    val description: String = "",
    val category: String = "FRUITS",
    val quantity: String = "",
    val unit: String = "kg",
    val expiryDate: String = "",
    val imageUrl: String = "",

    // Grocery's operating hours (loaded from profile, not editable here)
    val groceryHours: String? = null,

    // Errors
    val titleError: String? = null,
    val descriptionError: String? = null,
    val quantityError: String? = null,
    val unitError: String? = null,
    val expiryDateError: String? = null,

    // UI State
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCategoryDropdown: Boolean = false,
    val showUnitDropdown: Boolean = false,

    // AI Image Analysis
    val selectedImageUri: Uri? = null,
    val isAnalyzing: Boolean = false,
    val analysisResult: FoodAnalysisData? = null,
    val showImagePicker: Boolean = false,
    val analysisError: String? = null,

    // Multi-image
    val selectedImages: List<Uri> = emptyList(),  // first image is primary for upload

    // Draft & Preview
    val isPreviewMode: Boolean = false,
    val draftSavedAt: Long? = null
)