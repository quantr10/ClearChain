package com.clearchain.app.presentation.ngo.myrequests

import android.content.Context
import com.clearchain.app.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clearchain.app.data.remote.api.ReviewApi
import com.clearchain.app.data.remote.dto.SubmitReviewRequest
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.usecase.pickuprequest.CancelPickupRequestUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
import com.clearchain.app.domain.usecase.pickuprequest.GetMyPickupRequestsUseCase
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MyRequestsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getMyPickupRequestsUseCase: GetMyPickupRequestsUseCase,
    private val cancelPickupRequestUseCase: CancelPickupRequestUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase,
    private val signalRService: SignalRService,
    private val reviewApi: ReviewApi
) : ViewModel() {

    private val _state = MutableStateFlow(MyRequestsState())
    val state: StateFlow<MyRequestsState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    companion object {
        private const val MAX_UPLOAD_ATTEMPTS = 3
    }

    init {
        loadRequests()
        setupSignalR()  // ✅ ADD
    }

    // ✅ NEW: Setup SignalR real-time updates
    private fun setupSignalR() {
        viewModelScope.launch { signalRService.connect() }

        // Listen for status changes
    viewModelScope.launch {
        signalRService.pickupRequestStatusChanged.collect { notification ->
            // Auto-refresh list
            loadRequests()
            
            // Show notification to user
            val statusMessage = when (notification.newStatus.lowercase()) {
                "approved" -> context.getString(R.string.snack_your_request_approved)
                "ready" -> context.getString(R.string.snack_food_ready_pickup)
                "completed" -> context.getString(R.string.snack_pickup_completed)
                "rejected" -> context.getString(R.string.snack_request_rejected_by_grocery)
                else -> context.getString(R.string.snack_status_updated_to, notification.newStatus)
            }
            
            _uiEvent.send(UiEvent.ShowSnackbar(statusMessage))
        }
    }

    // Listen for cancellations
    viewModelScope.launch {
        signalRService.pickupRequestCancelled.collect { request ->
            loadRequests()
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled)))
        }
    }
}

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            signalRService.disconnect()
        }
    }

    fun onEvent(event: MyRequestsEvent) {
        when (event) {
            MyRequestsEvent.LoadRequests -> loadRequests()
            MyRequestsEvent.RefreshRequests -> refreshRequests()

            is MyRequestsEvent.SearchQueryChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                applyFilters()
            }
            is MyRequestsEvent.SortOptionChanged -> {
                _state.update { it.copy(selectedSort = event.option) }
                applyFilters()
            }
            is MyRequestsEvent.StatusFilterChanged -> {
                _state.update { it.copy(selectedStatus = event.status) }
                applyFilters()
            }

            is MyRequestsEvent.CancelRequest -> cancelRequest(event.requestId)
            
            is MyRequestsEvent.ConfirmPickupWithPhoto -> 
                confirmPickupWithPhoto(event.requestId, event.photoUri)

            MyRequestsEvent.RetryFailedUpload -> retryFailedUpload()
            MyRequestsEvent.DismissUploadError -> dismissUploadError()

            // Rate & review
            is MyRequestsEvent.ShowReviewDialog ->
                _state.update { it.copy(showReviewDialogForId = event.requestId, reviewRating = 5, reviewComment = "") }
            MyRequestsEvent.DismissReviewDialog ->
                _state.update { it.copy(showReviewDialogForId = null) }
            is MyRequestsEvent.ReviewRatingChanged ->
                _state.update { it.copy(reviewRating = event.rating) }
            is MyRequestsEvent.ReviewCommentChanged ->
                _state.update { it.copy(reviewComment = event.comment) }
            MyRequestsEvent.SubmitReview -> submitReview()

            // Dispute
            is MyRequestsEvent.DisputeRequest -> {
                viewModelScope.launch {
                    _uiEvent.send(UiEvent.Navigate(
                        com.clearchain.app.presentation.navigation.Screen.Dispute.createRoute(event.requestId)
                    ))
                }
            }

            // PDF receipt
            is MyRequestsEvent.GenerateReceipt -> {
                val request = _state.value.allRequests.find { it.id == event.requestId }
                if (request != null) generateReceipt(request)
            }

            MyRequestsEvent.ClearError -> {
                _state.update { it.copy(error = null) }
            }

            MyRequestsEvent.ShowFilterSheet ->
                _state.update { it.copy(showFilterSheet = true) }
            MyRequestsEvent.HideFilterSheet ->
                _state.update { it.copy(showFilterSheet = false) }
            is MyRequestsEvent.FilterCategoryChanged -> {
                _state.update { it.copy(filterCategory = event.category) }
                applyFilters()
            }
            is MyRequestsEvent.FilterPickupDatePresetChanged -> {
                _state.update { it.copy(filterPickupDatePreset = event.preset) }
                applyFilters()
            }
            MyRequestsEvent.ClearAdvancedFilters -> {
                _state.update { it.copy(filterCategory = null, filterPickupDatePreset = null) }
                applyFilters()
            }
        }
    }

    private fun loadRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = getMyPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isLoading = false
                        )
                    }
                    applyFilters()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: context.getString(R.string.error_load_requests)
                        )
                    }
                }
            )
        }
    }

    private fun refreshRequests() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }

            val result = getMyPickupRequestsUseCase()

            result.fold(
                onSuccess = { requests ->
                    _state.update {
                        it.copy(
                            allRequests = requests,
                            isRefreshing = false
                        )
                    }
                    applyFilters()
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_requests_refreshed)))
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = error.message ?: context.getString(R.string.error_refresh_requests)
                        )
                    }
                }
            )
        }
    }

    private fun applyFilters() {
        val current = _state.value
        var filtered = current.allRequests

        if (current.searchQuery.isNotBlank()) {
            val query = current.searchQuery.lowercase()
            filtered = filtered.filter { request ->
                request.listingTitle.lowercase().contains(query) ||
                        request.groceryName.lowercase().contains(query) ||
                        request.notes?.lowercase()?.contains(query) == true
            }
        }

        current.selectedStatus?.let { status ->
            filtered = filtered.filter { it.status.name == status }
        }

        current.filterCategory?.let { category ->
            filtered = filtered.filter { it.listingCategory == category }
        }

        current.filterPickupDatePreset?.let { preset ->
            val today = java.time.LocalDate.now()
            filtered = filtered.filter { request ->
                runCatching {
                    val pickupDate = java.time.LocalDate.parse(request.pickupDate.take(10))
                    when (preset) {
                        "TODAY" -> pickupDate == today
                        "WEEK"  -> !pickupDate.isBefore(today) && !pickupDate.isAfter(today.plusDays(7))
                        "MONTH" -> !pickupDate.isBefore(today) && !pickupDate.isAfter(today.plusDays(30))
                        "PAST"  -> pickupDate.isBefore(today)
                        else    -> true
                    }
                }.getOrDefault(true)
            }
        }

        filtered = when (current.selectedSort.value) {
            "date_desc" -> filtered.sortedByDescending { it.createdAt }
            "date_asc" -> filtered.sortedBy { it.createdAt }
            "pickup_date_asc" -> filtered.sortedBy { it.pickupDate }
            "pickup_date_desc" -> filtered.sortedByDescending { it.pickupDate }
            else -> filtered
        }

        _state.update { it.copy(filteredRequests = filtered) }
    }

    private fun cancelRequest(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = cancelPickupRequestUseCase(requestId)

            result.fold(
                onSuccess = {
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled)))
                    loadRequests()
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: context.getString(R.string.error_cancel_request_failed),
                            isLoading = false
                        )
                    }
                }
            )
        }
    }

    private fun confirmPickupWithPhoto(requestId: String, photoUri: Uri) {
        viewModelScope.launch {
            val currentAttempts = _state.value.uploadAttempts + 1
            
            _state.update { 
                it.copy(
                    isUploading = true, 
                    uploadError = null,
                    uploadAttempts = currentAttempts
                ) 
            }

            val result = confirmPickupUseCase(requestId, photoUri)

            result.fold(
                onSuccess = {
                    _state.update { 
                        it.copy(
                            isUploading = false,
                            uploadAttempts = 0,
                            failedUploadRequestId = null,
                            failedUploadPhotoUri = null
                        ) 
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_pickup_confirmed_photo)))
                    loadRequests()
                },
                onFailure = { error ->
                    val errorMessage = error.message ?: context.getString(R.string.error_upload_photo_failed)
                    val uploadError = if (currentAttempts < MAX_UPLOAD_ATTEMPTS) {
                        "$errorMessage\n${context.getString(R.string.msg_attempt_n_of_n, currentAttempts, MAX_UPLOAD_ATTEMPTS)}"
                    } else {
                        "$errorMessage\n${context.getString(R.string.msg_max_retry_reached)}"
                    }
                    _state.update {
                        it.copy(
                            isUploading           = false,
                            uploadError           = uploadError,
                            failedUploadRequestId = requestId,
                            failedUploadPhotoUri  = photoUri
                        )
                    }
                    _uiEvent.send(UiEvent.ShowSnackbar(errorMessage))
                }
            )
        }
    }

    private fun retryFailedUpload() {
        val currentState = _state.value
        
        if (currentState.failedUploadRequestId != null && 
            currentState.failedUploadPhotoUri != null &&
            currentState.uploadAttempts < MAX_UPLOAD_ATTEMPTS) {
            
            confirmPickupWithPhoto(
                currentState.failedUploadRequestId,
                currentState.failedUploadPhotoUri
            )
        } else {
            viewModelScope.launch {
                _uiEvent.send(
                    UiEvent.ShowSnackbar(context.getString(R.string.snack_cannot_retry))
                )
            }
        }
    }

    private fun dismissUploadError() {
        _state.update {
            it.copy(
                uploadError = null,
                uploadAttempts = 0,
                failedUploadRequestId = null,
                failedUploadPhotoUri = null
            )
        }
    }

    private fun generateReceipt(request: PickupRequest) {
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReceipt = true) }
            try {
                val uri = withContext(Dispatchers.IO) { buildReceiptPdf(request) }
                _uiEvent.send(UiEvent.ShareFile(uri, title = context.getString(R.string.snack_receipt_title, request.listingTitle)))
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_receipt_failed)))
            } finally {
                _state.update { it.copy(isGeneratingReceipt = false) }
            }
        }
    }

    private fun buildReceiptPdf(request: PickupRequest): Uri {
        val doc    = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page   = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 24f
            color    = Color.BLACK
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            textSize = 14f
            color    = Color.GRAY
        }
        val valuePaint = Paint().apply {
            textSize = 14f
            color    = Color.BLACK
        }
        val dividerPaint = Paint().apply {
            color       = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 60f
        canvas.drawText(context.getString(R.string.label_pickup_receipt), 40f, y, titlePaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 30f

        fun row(label: String, value: String) {
            canvas.drawText(label, 40f, y, labelPaint)
            canvas.drawText(value, 220f, y, valuePaint)
            y += 24f
        }

        row(context.getString(R.string.label_reference_id), request.id.take(16) + "…")
        row(context.getString(R.string.label_food_item),    request.listingTitle)
        row(context.getString(R.string.label_category),     request.listingCategory)
        row(context.getString(R.string.listing_quantity),   "${request.requestedQuantity}")
        row(context.getString(R.string.label_from),         request.groceryName)
        row(context.getString(R.string.label_pickup_date),  request.pickupDate)
        row(context.getString(R.string.label_pickup_time),  request.pickupTime)
        row(context.getString(R.string.label_status),       request.status.name)
        request.notes?.takeIf { it.isNotBlank() }?.let { row(context.getString(R.string.label_notes), it.take(60)) }

        y += 12f
        canvas.drawLine(40f, y, 555f, y, dividerPaint)
        y += 20f
        canvas.drawText(context.getString(R.string.pdf_generated_by), 40f, y,
            labelPaint.apply { textSize = 10f })

        doc.finishPage(page)

        val file = File(context.cacheDir, "receipt_${request.id.take(8)}.pdf")
        doc.writeTo(file.outputStream())
        doc.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun submitReview() {
        val s = _state.value
        if (s.showReviewDialogForId == null) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReview = true) }
            try {
                reviewApi.submitReview(
                    SubmitReviewRequest(
                        pickupRequestId = s.showReviewDialogForId!!,
                        rating          = s.reviewRating,
                        comment         = s.reviewComment.ifBlank { null }
                    )
                )
                _state.update { it.copy(isSubmittingReview = false, showReviewDialogForId = null) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_review_submitted)))
            } catch (e: Exception) {
                _state.update { it.copy(isSubmittingReview = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_submit_review_failed)))
            }
        }
    }
}