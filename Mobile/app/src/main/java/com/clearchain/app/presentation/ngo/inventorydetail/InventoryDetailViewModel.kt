package com.clearchain.app.presentation.ngo.inventorydetail

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.InventoryApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.dto.UpdateInventoryItemRequest
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.toDomain
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

// ═══ ViewModel ═══
@HiltViewModel
class InventoryDetailViewModel @Inject constructor(
    application: Application,
    private val inventoryApi: InventoryApi,
    private val pickupRequestApi: PickupRequestApi
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(InventoryDetailState())
    val state: StateFlow<InventoryDetailState> = _state.asStateFlow()

    fun loadItem(itemId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = inventoryApi.getInventoryItemById(itemId)
                val item = response.data.toDomain()
                _state.update { it.copy(item = item, isLoading = false) }
                item.pickupRequestId?.let { requestId ->
                    loadRelatedRequest(requestId)
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: getApplication<Application>().getString(R.string.error_load_item_failed), isLoading = false) }
            }
        }
    }

    fun startEdit() {
        val item = _state.value.item ?: return
        _state.update {
            it.copy(
                isEditing       = true,
                editProductName = item.productName,
                editCategory    = item.category,
                editQuantity    = item.quantity.toString(),
                editUnit        = item.unit,
                editExpiryDate  = item.expiryDate.take(10),
                saveError       = null
            )
        }
    }

    fun cancelEdit() = _state.update { it.copy(isEditing = false, saveError = null) }

    fun onEditProductNameChanged(v: String) = _state.update { it.copy(editProductName = v) }
    fun onEditCategoryChanged(v: String)    = _state.update { it.copy(editCategory = v) }
    fun onEditQuantityChanged(v: String)    = _state.update { it.copy(editQuantity = v) }
    fun onEditUnitChanged(v: String)        = _state.update { it.copy(editUnit = v) }
    fun onEditExpiryDateChanged(v: String)  = _state.update { it.copy(editExpiryDate = v) }

    fun saveEdit(itemId: String) {
        val s = _state.value
        val qty = s.editQuantity.toDoubleOrNull()
        if (s.editProductName.isBlank() || qty == null || s.editExpiryDate.isBlank()) {
            _state.update { it.copy(saveError = getApplication<Application>().getString(R.string.error_fill_required_fields)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, saveError = null) }
            try {
                val response = inventoryApi.updateInventoryItem(
                    itemId,
                    UpdateInventoryItemRequest(
                        productName = s.editProductName,
                        category    = s.editCategory,
                        quantity    = qty,
                        unit        = s.editUnit,
                        expiryDate  = s.editExpiryDate
                    )
                )
                _state.update { it.copy(item = response.data.toDomain(), isEditing = false, isSaving = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, saveError = e.message ?: getApplication<Application>().getString(R.string.error_item_save_failed)) }
            }
        }
    }

    fun showQrSheet()  = _state.update { it.copy(showQrSheet = true) }
    fun dismissQrSheet() = _state.update { it.copy(showQrSheet = false) }

    fun uploadPhoto(itemId: String, photoUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingPhoto = true, photoUploadError = null) }
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(photoUri)?.use { it.readBytes() }
                } ?: throw Exception("Could not read photo")
                val mimeType = getApplication<Application>().contentResolver
                    .getType(photoUri) ?: "image/jpeg"
                val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("photo", "photo.jpg", requestBody)
                val response = inventoryApi.uploadInventoryPhoto(itemId, part)
                _state.update { it.copy(item = response.data.toDomain(), isUploadingPhoto = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isUploadingPhoto = false, photoUploadError = e.message ?: getApplication<Application>().getString(R.string.error_upload_failed)) }
            }
        }
    }

    private fun loadRelatedRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRequest = true) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                _state.update { it.copy(relatedRequest = response.data.toDomain(), isLoadingRequest = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingRequest = false) }
            }
        }
    }
}