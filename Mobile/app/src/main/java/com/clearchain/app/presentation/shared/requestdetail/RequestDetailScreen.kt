package com.clearchain.app.presentation.shared.requestdetail

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ScreenPadding
import androidx.core.content.FileProvider
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.clearchain.app.data.remote.api.MessageApi
import com.clearchain.app.data.remote.api.OrganizationApi
import com.clearchain.app.data.remote.api.PickupRequestApi
import com.clearchain.app.data.remote.api.PublicProfileData
import com.clearchain.app.data.remote.api.ReviewApi
import com.clearchain.app.data.remote.dto.MessageData
import com.clearchain.app.data.remote.dto.ReviewData
import com.clearchain.app.data.remote.dto.SendMessageRequest
import com.clearchain.app.data.remote.dto.SubmitReviewRequest
import com.clearchain.app.data.remote.dto.toDomain
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestItem
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.clearchain.app.data.remote.signalr.SignalRService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.graphics.Color as AColor

private const val PICKUP_CHECKLIST_SIZE = 5

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â State ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
data class RequestDetailState(
    val request: PickupRequest? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserType: OrganizationType? = null,
    val isActionLoading: Boolean = false,
    val actionError: String? = null,
    val showRejectDialog: Boolean = false,
    val checkedItems: Set<Int> = emptySet(),
    val messages: List<MessageData> = emptyList(),
    val messageInput: String = "",
    val isSendingMessage: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val myReview: ReviewData? = null,
    val ngoReview: ReviewData? = null,
    val isLoadingReview: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val showAutoRatingSheet: Boolean = false,
    val showRatingSheet: Boolean = false,
    val isGeneratingReceipt: Boolean = false,
    val groceryProfile: PublicProfileData? = null,
) {
    val allChecked: Boolean get() = checkedItems.size == PICKUP_CHECKLIST_SIZE
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â ViewModel ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pickupRequestApi: PickupRequestApi,
    private val messageApi: MessageApi,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase,
    private val reviewApi: ReviewApi,
    private val organizationApi: OrganizationApi,
    private val signalRService: SignalRService
) : ViewModel() {

    private val _state = MutableStateFlow(RequestDetailState())
    val state: StateFlow<RequestDetailState> = _state.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // In-session guard: prevents the auto-sheet from firing more than once
    // even if loadMyReview is called multiple times (e.g. after submitReview).
    private var autoSheetShownFor: String? = null

    /** Room this screen joined, so it can be left when the screen goes away. */
    private var joinedRequestId: String? = null

    init {
        viewModelScope.launch {
            getCurrentUserUseCase().first()?.let { user ->
                _state.update { it.copy(currentUserId = user.id, currentUserType = user.type) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        joinedRequestId?.let { requestId ->
            CoroutineScope(Dispatchers.IO).launch {
                signalRService.leavePickupRequestRoom(requestId)
            }
        }
    }

    /**
     * Keeps this screen in step with the other party. Both sides of a pickup are usually looking
     * at this screen at the same time — the grocery marking it ready while the NGO waits — so a
     * status change has to land here without a manual refresh.
     */
    private fun observeSignalR(requestId: String) {
        if (joinedRequestId == requestId) return

        // The server broadcasts detail updates to `pickup_{id}`, a group with no members until
        // a client joins it. Nothing joined it before, so those broadcasts went nowhere.
        viewModelScope.launch { signalRService.joinPickupRequestRoom(requestId) }
        joinedRequestId = requestId

        viewModelScope.launch {
            signalRService.pickupRequestUpdated.collect { data ->
                if (data.id == requestId) refreshRequest(requestId)
            }
        }
        viewModelScope.launch {
            signalRService.pickupRequestStatusChanged.collect { notification ->
                if (notification.request.id == requestId) refreshRequest(requestId)
            }
        }
        viewModelScope.launch {
            signalRService.pickupRequestCancelled.collect { data ->
                if (data.id == requestId) refreshRequest(requestId)
            }
        }
    }

    /** Reloads quietly — no spinner, since the screen already has content on it. */
    private fun refreshRequest(requestId: String) {
        viewModelScope.launch {
            runCatching { pickupRequestApi.getPickupRequestById(requestId) }
                .onSuccess { response ->
                    _state.update { it.copy(request = response.data.toDomain()) }
                }
        }
    }

    fun loadRequest(requestId: String) {
        observeSignalR(requestId)
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = pickupRequestApi.getPickupRequestById(requestId)
                val req = response.data.toDomain()
                _state.update { it.copy(request = req, isLoading = false) }
                loadMessages(requestId)
                if (_state.value.currentUserType == OrganizationType.NGO) {
                    loadGroceryProfile(req.groceryId)
                }
                if (req.status == PickupRequestStatus.COMPLETED) {
                    if (_state.value.currentUserType == OrganizationType.NGO) {
                        loadMyReview(requestId)
                    } else if (_state.value.currentUserType == OrganizationType.GROCERY) {
                        loadNgoReview(requestId, req.groceryId)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load request", isLoading = false) }
            }
        }
    }

    fun loadMyReview(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingReview = true) }
            try {
                val response = reviewApi.getMyReviews()
                val mine = response.data.find { it.pickupRequestId == requestId }
                _state.update { it.copy(myReview = mine, isLoadingReview = false) }

                // Only evaluate the auto-sheet once per ViewModel instance.
                // autoSheetShownFor acts as an in-session guard so coroutine races
                // can never trigger the sheet twice even if loadMyReview is called again.
                if (autoSheetShownFor != requestId) {
                    autoSheetShownFor = requestId
                    val prefs = context.getSharedPreferences("request_detail_prefs", Context.MODE_PRIVATE)
                    val seenKey = "seen_complete_$requestId"
                    if (!prefs.getBoolean(seenKey, false)) {
                        // commit() writes synchronously so the flag survives rapid ViewModel recreation.
                        prefs.edit().putBoolean(seenKey, true).commit()
                        _state.update { it.copy(showAutoRatingSheet = true) }
                    }
                }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingReview = false) }
            }
        }
    }

    private fun loadNgoReview(requestId: String, groceryId: String) {
        viewModelScope.launch {
            try {
                val response = reviewApi.getReviewsForOrganization(groceryId)
                val review = response.data.find { it.pickupRequestId == requestId }
                _state.update { it.copy(ngoReview = review) }
            } catch (_: Exception) {}
        }
    }

    fun submitReview(requestId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReview = true) }
            try {
                reviewApi.submitReview(SubmitReviewRequest(requestId, rating, comment?.ifBlank { null }))
                loadMyReview(requestId)
                _state.update { it.copy(isSubmittingReview = false, showAutoRatingSheet = false, showRatingSheet = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_review_submitted)))
            } catch (e: Exception) {
                _state.update { it.copy(isSubmittingReview = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: "Failed to submit review"))
            }
        }
    }

    fun dismissAutoRatingSheet() = _state.update { it.copy(showAutoRatingSheet = false) }
    fun openRatingSheet()       = _state.update { it.copy(showRatingSheet = true) }
    fun closeRatingSheet()      = _state.update { it.copy(showRatingSheet = false) }

    fun generateReceipt() {
        val req = _state.value.request ?: return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReceipt = true) }
            try {
                val uri = withContext(Dispatchers.IO) { buildReceiptPdf(req) }
                _uiEvent.send(UiEvent.ShareFile(uri, title = context.getString(R.string.snack_receipt_title, req.listingTitle)))
            } catch (_: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_receipt_failed)))
            } finally {
                _state.update { it.copy(isGeneratingReceipt = false) }
            }
        }
    }

    private fun buildReceiptPdf(request: PickupRequest): Uri {
        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page     = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 24f; color = AColor.BLACK; isFakeBoldText = true }
        val labelPaint = Paint().apply { textSize = 14f; color = AColor.GRAY }
        val valuePaint = Paint().apply { textSize = 14f; color = AColor.BLACK }
        val divPaint   = Paint().apply { color = AColor.LTGRAY; strokeWidth = 1f }

        var y = 60f
        canvas.drawText(context.getString(R.string.label_pickup_receipt), 40f, y, titlePaint)
        y += 8f; canvas.drawLine(40f, y, 555f, y, divPaint); y += 30f

        fun row(label: String, value: String) {
            canvas.drawText(label, 40f, y, labelPaint); canvas.drawText(value, 220f, y, valuePaint); y += 24f
        }
        row(context.getString(R.string.label_reference_id), request.id.take(16) + "ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¦")
        row(context.getString(R.string.label_food_item),    request.listingTitle)
        row(context.getString(R.string.label_category),     request.listingCategory)
        row(context.getString(R.string.listing_quantity),   "${request.requestedQuantity}")
        row(context.getString(R.string.label_from),         request.groceryName)
        row(context.getString(R.string.label_pickup_date),  request.pickupDate)
        row(context.getString(R.string.label_pickup_time),  request.pickupTime)
        row(context.getString(R.string.label_status),       request.status.name)
        request.notes?.takeIf { it.isNotBlank() }?.let { row(context.getString(R.string.label_notes), it.take(60)) }

        y += 12f; canvas.drawLine(40f, y, 555f, y, divPaint); y += 20f
        canvas.drawText(context.getString(R.string.pdf_generated_by), 40f, y, labelPaint.apply { textSize = 10f })
        doc.finishPage(page)

        val file = File(context.cacheDir, "receipt_${request.id.take(8)}.pdf")
        doc.writeTo(file.outputStream()); doc.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun loadGroceryProfile(groceryId: String) {
        viewModelScope.launch {
            runCatching { organizationApi.getPublicProfile(groceryId).data }
                .onSuccess { profile -> _state.update { it.copy(groceryProfile = profile) } }
        }
    }

    fun approve(requestId: String) = runAction(requestId) {
        pickupRequestApi.approvePickupRequest(requestId)
        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_approved)))
    }

    fun markReady(requestId: String) = runAction(requestId) {
        pickupRequestApi.markReadyForPickup(requestId)
        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_marked_ready)))
    }

    fun reject(requestId: String) {
        _state.update { it.copy(showRejectDialog = false) }
        runAction(requestId) {
            pickupRequestApi.updatePickupRequestStatus(
                requestId,
                com.clearchain.app.data.remote.dto.UpdatePickupRequestStatusRequest("rejected")
            )
            _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_rejected)))
        }
    }

    fun cancel(requestId: String) = runAction(requestId) {
        pickupRequestApi.cancelPickupRequest(requestId)
        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_request_cancelled)))
    }

    fun confirmPickup(requestId: String, photoUri: Uri) = runAction(requestId) {
        confirmPickupUseCase(requestId, photoUri).getOrThrow()
        _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_pickup_confirmed)))
    }

    fun showRejectDialog()    = _state.update { it.copy(showRejectDialog = true) }
    fun dismissRejectDialog() = _state.update { it.copy(showRejectDialog = false) }

    fun loadMessages(requestId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingMessages = true) }
            try {
                val response = messageApi.getMessages(requestId)
                _state.update { it.copy(messages = response.data, isLoadingMessages = false) }
            } catch (_: Exception) {
                _state.update { it.copy(isLoadingMessages = false) }
            }
        }
    }

    fun onMessageInputChanged(text: String) = _state.update { it.copy(messageInput = text) }

    fun sendMessage(requestId: String) {
        val content = _state.value.messageInput.trim()
        if (content.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSendingMessage = true, messageInput = "") }
            try {
                messageApi.sendMessage(requestId, SendMessageRequest(content))
                loadMessages(requestId)
            } catch (_: Exception) {}
            _state.update { it.copy(isSendingMessage = false) }
        }
    }

    private fun runAction(requestId: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isActionLoading = true, actionError = null) }
            try {
                block()
                loadRequest(requestId)
            } catch (e: Exception) {
                val msg = e.message ?: "Action failed"
                _state.update { it.copy(actionError = msg) }
                _uiEvent.send(UiEvent.ShowSnackbar(msg))
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Screen ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onNavigateBack: () -> Unit,
    onNavigateToDispute: (String) -> Unit = {},
    onNavigateToPublicProfile: (String) -> Unit = {},
    onNavigateToListing: (String) -> Unit = {},
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showChatSheet by remember { mutableStateOf(false) }

    val req        = state.request
    val isGrocery  = state.currentUserType == OrganizationType.GROCERY
    val isNgo      = state.currentUserType == OrganizationType.NGO
    val isMyRequest = req != null && when {
        isGrocery -> req.groceryId == state.currentUserId
        isNgo     -> req.ngoId == state.currentUserId
        else      -> false
    }
    val isChatVisible = isMyRequest && req != null &&
        req.status != PickupRequestStatus.COMPLETED &&
        req.status != PickupRequestStatus.CANCELLED &&
        req.status != PickupRequestStatus.REJECTED

    var showChecklistSheet by remember { mutableStateOf(false) }
    var showPhotoPicker    by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) { viewModel.loadRequest(requestId) }
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.ShareFile -> {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, event.title))
                }
                else -> {}
            }
        }
    }

    // Auto rating sheet on first view after completion (NGO only)
    if (state.showAutoRatingSheet && req != null && isNgo) {
        RatingSheet(
            myReview     = state.myReview,
            isSubmitting = state.isSubmittingReview,
            onDismiss    = { viewModel.dismissAutoRatingSheet() },
            onSubmit     = { rating, comment -> viewModel.submitReview(requestId, rating, comment) }
        )
    }

    // Manual rating sheet from "Show" button (NGO only)
    if (state.showRatingSheet && req != null && isNgo) {
        RatingSheet(
            myReview     = state.myReview,
            isSubmitting = state.isSubmittingReview,
            onDismiss    = { viewModel.closeRatingSheet() },
            onSubmit     = { rating, comment -> viewModel.submitReview(requestId, rating, comment) }
        )
    }

    // Step 1 ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Checklist verification sheet
    if (showChecklistSheet) {
        PickupChecklistSheet(
            onDismiss = { showChecklistSheet = false },
            onNext    = { showChecklistSheet = false; showPhotoPicker = true }
        )
    }

    // Steps 2 and 3 - photo source picker + preview
    if (showPhotoPicker) {
        PhotoPickerDialog(
            onPhotoSelected = { uri ->
                viewModel.confirmPickup(requestId, uri)
                showPhotoPicker = false
            },
            onDismiss = { showPhotoPicker = false },
            title = stringResource(R.string.label_add_photo_proof),
            message = stringResource(R.string.msg_choose_photo_source),
            previewMessage = stringResource(R.string.msg_submit_photo_proof)
        )
    }

    // Reject dialog
    if (state.showRejectDialog) {
        ConfirmDialog(
            onDismiss = { viewModel.dismissRejectDialog() },
            onConfirm = { viewModel.reject(requestId) },
            icon = Icons.Default.Cancel,
            title = stringResource(R.string.label_reject_request),
            message = stringResource(R.string.msg_reject_request_notice),
            confirmLabel = stringResource(R.string.reject),
            dismissLabel = stringResource(R.string.cancel),
            isDestructive = true
        )
    }

    // Chat bottom sheet
    if (showChatSheet) {
        ModalBottomSheet(onDismissRequest = { showChatSheet = false }) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.label_messages_section),
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ChatSection(
                    messages       = state.messages,
                    currentUserId  = state.currentUserId ?: "",
                    messageInput   = state.messageInput,
                    isSending      = state.isSendingMessage,
                    isLoading      = state.isLoadingMessages,
                    onInputChanged = { viewModel.onMessageInputChanged(it) },
                    onSend         = { viewModel.sendMessage(requestId) }
                )
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isChatVisible) {
                SmallFloatingActionButton(
                    onClick            = { showChatSheet = true; viewModel.loadMessages(requestId) },
                    containerColor     = MaterialTheme.colorScheme.primary,
                    contentColor       = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.label_messages_section))
                }
            }
        },
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenTitleRow(
                title = stringResource(R.string.title_request_details),
                onBack = onNavigateBack,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> EmptyState(
                    icon        = Icons.Default.ErrorOutline,
                    title       = stringResource(R.string.error_generic),
                    subtitle    = state.error,
                    actionLabel = stringResource(R.string.retry),
                    onAction    = { viewModel.loadRequest(requestId) }
                )
                req != null -> RequestDetailContent(
                    req               = req,
                    state             = state,
                    isGrocery         = isGrocery,
                    isNgo             = isNgo,
                    isMyRequest       = isMyRequest,
                    showDisputeButton = isMyRequest &&
                        req.status != PickupRequestStatus.CANCELLED &&
                        req.status != PickupRequestStatus.REJECTED,
                    onApprove         = { viewModel.approve(requestId) },
                    onReject          = { viewModel.showRejectDialog() },
                    onMarkReady       = { viewModel.markReady(requestId) },
                    onCancel          = { viewModel.cancel(requestId) },
                    onConfirmPickup           = { showChecklistSheet = true },
                    onNavigateToPublicProfile = onNavigateToPublicProfile,
                    onNavigateToListing       = onNavigateToListing,
                    onGenerateReceipt         = { viewModel.generateReceipt() },
                    onShowRatingSheet         = { viewModel.openRatingSheet() },
                    groceryProfile            = state.groceryProfile
                )
            }
            }
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Main content ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun RequestDetailContent(
    req: PickupRequest,
    state: RequestDetailState,
    isGrocery: Boolean,
    isNgo: Boolean,
    isMyRequest: Boolean,
    showDisputeButton: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkReady: () -> Unit,
    onCancel: () -> Unit,
    onConfirmPickup: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit,
    onNavigateToListing: (String) -> Unit,
    onGenerateReceipt: () -> Unit,
    onShowRatingSheet: () -> Unit,
    groceryProfile: PublicProfileData? = null,
) {
    val context = LocalContext.current
    var showDisputeSheet by remember { mutableStateOf(false) }
    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ Expiry computation (same logic as RequestCard) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    val daysUntilExpiry: Long? = remember(req.listingExpiryDate) {
        val raw = req.listingExpiryDate ?: return@remember null
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw)!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(date.time - today.time)
        } catch (_: Exception) { null }
    }
    val expiryColor = daysUntilExpiry?.let { when {
        it <= 0L -> MaterialTheme.colorScheme.error
        it <= 3L -> Color(0xFFE65100)
        else     -> MaterialTheme.colorScheme.onSurfaceVariant
    }} ?: MaterialTheme.colorScheme.onSurfaceVariant

    val expiryText: String? = daysUntilExpiry?.let {
        when {
            it < 0  -> stringResource(R.string.listing_expired_label)
            it == 0L -> stringResource(R.string.listing_expires_today)
            it == 1L -> stringResource(R.string.listing_expires_tomorrow)
            it in 2..3 -> stringResource(R.string.listing_expires_in_days, it.toInt())
            else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(req.listingExpiryDate!!))
        }
    }

    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ Pickup timestamp ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    val timestampText = stringResource(
        R.string.label_pickup_on_at,
        DateTimeUtils.formatDate(req.pickupDate),
        req.pickupTime
    )

    val handlingParts = buildList {
        if (req.requiresRefrigeration) add(stringResource(R.string.note_needs_refrigeration))
        if (req.isFragile)             add(stringResource(R.string.note_fragile_items))
        if (req.isHeavy)               add(stringResource(R.string.note_heavy_load))
        req.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        //ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 1. Avatar + party name card (with action buttons top-right) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        val showReceiptBtn = isMyRequest && req.status == PickupRequestStatus.COMPLETED
        val partyName = if (isGrocery) req.ngoName else req.groceryName
        val partyType = if (isGrocery) stringResource(R.string.label_ngo_party)
                        else stringResource(R.string.label_grocery_party)
        val partyId   = if (isGrocery) req.ngoId else req.groceryId
        val partyAvatar = if (isGrocery) req.ngoProfilePictureUrl
                          else req.groceryProfilePictureUrl

        Card(
            modifier  = Modifier.fillMaxWidth().height(148.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(12.dp)
            ) {
                if (showReceiptBtn || showDisputeButton) {
                    Row(
                        modifier              = Modifier.align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        if (showReceiptBtn) {
                            ImageActionButton(
                                icon    = Icons.Default.Receipt,
                                label   = stringResource(R.string.cd_download_receipt),
                                loading = state.isGeneratingReceipt,
                                onClick = onGenerateReceipt
                            )
                        }
                        if (showDisputeButton) {
                            ImageActionButton(
                                icon    = Icons.Default.Flag,
                                label   = stringResource(R.string.action_file_dispute),
                                onClick = { showDisputeSheet = true }
                            )
                        }
                    }
                }
                Column(
                    modifier            = Modifier.align(Alignment.BottomCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick  = { onNavigateToPublicProfile(partyId) },
                        modifier = Modifier.size(64.dp),
                        shape    = CircleShape,
                        color    = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        if (!partyAvatar.isNullOrBlank()) {
                            AsyncImage(
                                model              = partyAvatar,
                                contentDescription = partyName,
                                modifier           = Modifier.fillMaxSize(),
                                contentScale       = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    partyName.take(1).uppercase(),
                                    style      = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(partyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(partyType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showDisputeSheet) {
            DisputeSheet(
                isGrocery = isGrocery,
                onDismiss = { showDisputeSheet = false }
            )
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 2. Lifecycle timeline (already a Card) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        LifecycleTimeline(request = req)

        // Product name
        Text(
            req.listingTitle,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.fillMaxWidth()
        )

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 4. Description card ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (!req.listingDescription.isNullOrBlank()) {
            SectionCard(stringResource(R.string.label_description)) {
                Text(
                    req.listingDescription.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 5. Request Info card ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (req.items.isNotEmpty()) {
            SectionCard(stringResource(R.string.cart_requested_items)) {
                req.items.forEachIndexed { index, item ->
                    RequestedItemRow(
                        item = item,
                        showDivider = index != req.items.lastIndex
                    )
                }
            }
        }

        SectionCard(stringResource(R.string.label_request_information)) {
            if (expiryText != null) {
                CompactDetailRow(Icons.Default.CalendarToday, expiryText, expiryColor)
            }
            CompactDetailRow(Icons.Default.AccessTime, timestampText, MaterialTheme.colorScheme.onSurfaceVariant)
            if (handlingParts.isNotEmpty()) {
                CompactDetailRow(Icons.AutoMirrored.Filled.StickyNote2, handlingParts.joinToString(" \u00B7 "), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 5b. About Us card (NGO only) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (isNgo && groceryProfile != null) {
            val address = groceryProfile.address?.takeIf { it.isNotBlank() }
                ?: groceryProfile.location?.takeIf { it.isNotBlank() }

            SectionCard(stringResource(R.string.section_about_us)) {
                if (address != null) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Place, null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            address,
                            style    = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color    = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ClearChainActionIconButton(
                            icon = Icons.Default.Navigation,
                            contentDescription = stringResource(R.string.action_get_directions),
                            onClick = {
                                val encoded = Uri.encode(address)
                                val uri = Uri.parse("geo:0,0?q=$encoded")
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, uri).apply {
                                            setPackage("com.google.android.apps.maps")
                                        }
                                    )
                                }.onFailure {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$encoded"))
                                    )
                                }
                            }
                        )
                    }
                }

                groceryProfile.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Row(
                        modifier              = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        },
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Phone, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            phone,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 7. Action buttons ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (isMyRequest) {
            if (isNgo && req.status == PickupRequestStatus.READY) {
                ClearChainButton(
                    text = stringResource(R.string.action_confirm_pickup_photo),
                    onClick = onConfirmPickup,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PhotoCamera,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                )
            }

            if (isGrocery && req.status == PickupRequestStatus.PENDING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClearChainButton(
                        text = stringResource(R.string.approve),
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check
                    )
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.reject),
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Close,
                        contentColor = MaterialTheme.colorScheme.error,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    )
                }
            }

            if (isGrocery && req.status == PickupRequestStatus.APPROVED) {
                ClearChainButton(
                    text = stringResource(R.string.action_mark_ready),
                    onClick = onMarkReady,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Check
                )
            }

            if (isNgo && req.status == PickupRequestStatus.PENDING) {
                ClearChainOutlinedButton(
                    text = stringResource(R.string.cancel_request),
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Cancel,
                    contentColor = MaterialTheme.colorScheme.error,
                    fillMaxWidth = true,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                )
            }
        }

        if (state.isActionLoading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 8. Rating card (NGO: submit rating; Grocery: view received rating) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (req.status == PickupRequestStatus.COMPLETED && isMyRequest && isNgo) {
            SectionCard(stringResource(R.string.label_rate_experience)) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.hint_pickup_experience),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    ClearChainActionIconButton(
                        icon = Icons.Default.Star,
                        contentDescription = stringResource(R.string.action_show),
                        onClick = onShowRatingSheet
                    )
                }
            }
        }

        if (req.status == PickupRequestStatus.COMPLETED && isMyRequest && isGrocery) {
            SectionCard(stringResource(R.string.label_ngo_rating)) {
                val review = state.ngoReview
                if (review != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            repeat(5) { i ->
                                Icon(
                                    imageVector        = if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    modifier           = Modifier.size(28.dp),
                                    tint               = if (i < review.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.label_reviewed_by, review.reviewerName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!review.comment.isNullOrBlank()) {
                            Surface(
                                color    = MaterialTheme.colorScheme.surfaceVariant,
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "\"${review.comment}\"",
                                    style    = MaterialTheme.typography.bodyMedium,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.StarBorder, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.hint_not_yet_rated),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 9. Proof Photo card (completed) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
        if (req.status == PickupRequestStatus.COMPLETED && req.proofPhotoUrl != null) {
            SectionCard(stringResource(R.string.proof_photo)) {
                ZoomablePhoto(url = req.proofPhotoUrl)
            }
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Circular overlay action button ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun ImageActionButton(
    icon:    ImageVector,
    label:   String,
    tint:    Color   = MaterialTheme.colorScheme.onSurfaceVariant,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick         = onClick,
        modifier        = Modifier.size(24.dp),
        shape           = CircleShape,
        color           = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = tint)
            } else {
                Icon(icon, label, Modifier.size(18.dp), tint = tint)
            }
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Section card ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                title,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Compact section label ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun RequestedItemRow(
    item: PickupRequestItem,
    showDivider: Boolean
) {
    val quantityText = if (item.listingUnit.isNotBlank()) {
        "${item.requestedQuantity} ${item.listingUnit}"
    } else {
        item.requestedQuantity.toString()
    }
    val expiryText = item.listingExpiryDate
        ?.let { stringResource(R.string.label_expires_date, DateTimeUtils.formatDate(it)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(
                imageUrl = item.listingPhotoUrl,
                contentDescription = item.listingTitle,
                size = 48.dp
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.listingTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                expiryText?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = quantityText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Compact detail row ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun CompactDetailRow(
    icon:      androidx.compose.ui.graphics.vector.ImageVector,
    text:      String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    bold:      Boolean = true
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = textColor)
        Text(
            text,
            style      = MaterialTheme.typography.labelSmall,
            color      = textColor,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Zoomable proof photo ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun ZoomablePhoto(url: String) {
    var scale  by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale  = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }
    LaunchedEffect(scale) { if (scale <= 1f) offset = Offset.Zero }

    // Clipped Box, not a Card: the only caller puts this inside a SectionCard, so a
    // Card here would be a second elevated surface inside that one.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .transformable(transformState)
    ) {
        AsyncImage(
            model              = url,
            contentDescription = stringResource(R.string.cd_proof_of_pickup),
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX       = scale,
                    scaleY       = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Crop
        )
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Lifecycle Timeline ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun LifecycleTimeline(request: PickupRequest) {
    val icons = listOf(
        Icons.AutoMirrored.Filled.Send,
        Icons.Default.CheckCircle,
        Icons.Default.Inventory2,
        Icons.Default.TaskAlt
    )

    val currentIndex = when (request.status) {
        PickupRequestStatus.PENDING   -> 0
        PickupRequestStatus.APPROVED  -> 1
        PickupRequestStatus.READY     -> 2
        PickupRequestStatus.COMPLETED -> 3
        PickupRequestStatus.CANCELLED,
        PickupRequestStatus.REJECTED  -> -1
    }

    val green = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outlineVariant

    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ Current-stage text (only for active statuses) ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    val statusTitle = when (request.status) {
        PickupRequestStatus.PENDING   -> stringResource(R.string.label_status_submitted)
        PickupRequestStatus.APPROVED  -> stringResource(R.string.label_status_approved)
        PickupRequestStatus.READY     -> stringResource(R.string.label_status_ready)
        PickupRequestStatus.COMPLETED -> stringResource(R.string.label_status_completed)
        else -> null
    }
    val statusSub = when (request.status) {
        PickupRequestStatus.PENDING  -> stringResource(
            R.string.label_requested_on_at,
            DateTimeUtils.formatDate(request.createdAt),
            DateTimeUtils.formatTime(request.createdAt)
        )
        PickupRequestStatus.APPROVED -> stringResource(
            R.string.label_approved_on,
            DateTimeUtils.formatDate(request.pickupDate),
            request.pickupTime
        )
        PickupRequestStatus.READY    -> stringResource(
            R.string.label_ready_pickup_by,
            DateTimeUtils.formatDate(request.pickupDate),
            request.pickupTime
        )
        PickupRequestStatus.COMPLETED -> {
            val ts = request.confirmedReceivedAt ?: request.markedPickedUpAt ?: request.createdAt
            stringResource(
                R.string.label_completed_on_at,
                DateTimeUtils.formatDate(ts),
                DateTimeUtils.formatTime(ts)
            )
        }
        else -> null
    }

    Card(
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status sentence block
            if (statusTitle != null && statusSub != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        statusTitle,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        statusSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // Horizontal icon bar
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icons.forEachIndexed { index, icon ->
                    val isReached = currentIndex >= 0 && index <= currentIndex
                    val iconTint  = if (isReached) green else muted
                    val iconBg    = if (isReached) green.copy(alpha = 0.15f) else muted.copy(alpha = 0.15f)

                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape    = CircleShape,
                        color    = iconBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector        = icon,
                                contentDescription = null,
                                modifier           = Modifier.size(20.dp),
                                tint               = iconTint
                            )
                        }
                    }

                    if (index < icons.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(if (currentIndex >= 0 && index < currentIndex) green else muted)
                        )
                    }
                }
            }
        }
    }

    // Cancelled / Rejected banner
    if (request.status == PickupRequestStatus.CANCELLED || request.status == PickupRequestStatus.REJECTED) {
        Surface(
            color    = MaterialTheme.colorScheme.errorContainer,
            shape    = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier              = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Cancel, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    if (request.status == PickupRequestStatus.CANCELLED)
                        stringResource(R.string.label_cancelled_on, DateTimeUtils.formatDateTime(request.createdAt))
                    else
                        stringResource(R.string.label_rejected_on, DateTimeUtils.formatDateTime(request.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Chat Section ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@Composable
private fun ChatSection(
    messages:      List<MessageData>,
    currentUserId: String,
    messageInput:  String,
    isSending:     Boolean,
    isLoading:     Boolean,
    onInputChanged: (String) -> Unit,
    onSend:         () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val inputInteractionSource = remember { MutableInteractionSource() }
    val inputFocused by inputInteractionSource.collectIsFocusedAsState()
    val inputBorderColor = if (inputFocused) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.outlineVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (messages.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.msg_no_messages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state               = listState,
                modifier            = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg, isMine = msg.senderId == currentUserId)
                }
            }
        }

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = messageInput,
                onValueChange = onInputChanged,
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .border(1.dp, inputBorderColor, ShapeMedium)
                    .padding(horizontal = 12.dp),
                textStyle = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                interactionSource = inputInteractionSource,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send, keyboardType = KeyboardType.Text),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                enabled = !isSending,
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.weight(1f)) {
                            if (messageInput.isEmpty()) {
                                Text(
                                    stringResource(R.string.hint_type_message),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
            Surface(
                onClick = onSend,
                enabled = messageInput.isNotBlank() && !isSending,
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (isSending) {
                        CircularProgressIndicator(Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            stringResource(R.string.cd_send),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: MessageData, isMine: Boolean) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor   = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        if (!isMine) {
            Text(message.senderName, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
        Surface(
            shape = RoundedCornerShape(
                topStart    = if (isMine) 16.dp else 4.dp,
                topEnd      = if (isMine) 4.dp else 16.dp,
                bottomStart = 16.dp, bottomEnd = 16.dp
            ),
            color    = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Text(
            DateTimeUtils.getTimeAgo(message.sentAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp)
        )
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Dispute Sheet ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisputeSheet(isGrocery: Boolean, onDismiss: () -> Unit) {
    var selectedReason by remember { mutableStateOf("") }
    var statement      by remember { mutableStateOf("") }

    val ngoReasons = listOf(
        stringResource(R.string.dispute_reason_poor_condition),
        stringResource(R.string.dispute_reason_wrong_items),
        stringResource(R.string.dispute_reason_quantity),
        stringResource(R.string.dispute_reason_expired),
        stringResource(R.string.dispute_reason_not_available),
        stringResource(R.string.dispute_reason_other)
    )
    val groceryReasons = listOf(
        stringResource(R.string.dispute_reason_no_show),
        stringResource(R.string.dispute_reason_time_violation),
        stringResource(R.string.dispute_reason_damage),
        stringResource(R.string.dispute_reason_wrong_qty_taken),
        stringResource(R.string.dispute_reason_behavior),
        stringResource(R.string.dispute_reason_other)
    )
    val reasons = if (isGrocery) groceryReasons else ngoReasons

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.open_dispute),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            // Info banner
            Surface(
                color  = MaterialTheme.colorScheme.secondaryContainer,
                shape  = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        Modifier.size(16.dp).padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        stringResource(R.string.dispute_info_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Reason selection
            Text(
                stringResource(R.string.dispute_reason),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            reasons.forEach { reason ->
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable { selectedReason = reason }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected  = selectedReason == reason,
                        onClick   = { selectedReason = reason }
                    )
                    Text(reason, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Statement field
            Text(
                stringResource(R.string.dispute_statement_label),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedTextField(
                value         = statement,
                onValueChange = { statement = it },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text(stringResource(R.string.dispute_statement_hint)) },
                minLines      = 3,
                maxLines      = 6,
                shape         = RoundedCornerShape(12.dp)
            )

            // Submit
            ClearChainButton(
                text = stringResource(R.string.dispute_submit),
                onClick = onDismiss,
                enabled = selectedReason.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Flag,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    }
}

// ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â Rating Sheet ÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚ÂÃƒÂ¢Ã¢â‚¬Â¢Ã‚Â
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RatingSheet(
    myReview:     ReviewData?,
    isSubmitting: Boolean,
    onDismiss:    () -> Unit,
    onSubmit:     (Int, String?) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(myReview?.rating ?: 0) }
    var comment        by remember { mutableStateOf(myReview?.comment ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.label_rate_experience),
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }

            if (myReview != null) {
                // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ Read-only: already rated ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        stringResource(R.string.your_rating),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(5) { i ->
                            Icon(
                                imageVector        = if (i < myReview.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.cd_star_n, i + 1),
                                modifier           = Modifier.size(36.dp),
                                tint               = if (i < myReview.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (!myReview.comment.isNullOrBlank()) {
                        Surface(
                            color    = MaterialTheme.colorScheme.surfaceVariant,
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                myReview.comment,
                                style    = MaterialTheme.typography.bodyMedium,
                                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            } else {
                // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ Interactive: not yet rated ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
                Text(
                    stringResource(R.string.rate_and_review),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        IconButton(
                            onClick  = { selectedRating = i + 1 },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector        = if (i < selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.cd_star_n, i + 1),
                                modifier           = Modifier.size(36.dp),
                                tint               = if (i < selectedRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { comment = it },
                    modifier      = Modifier.fillMaxWidth(),
                    label         = {
                        OptionalFieldLabel(
                            text = stringResource(R.string.label_comments_optional),
                            isOptional = true
                        )
                    },
                    placeholder   = { Text(stringResource(R.string.hint_pickup_experience)) },
                    minLines      = 2,
                    maxLines      = 4,
                    shape         = RoundedCornerShape(12.dp)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.cancel),
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    ClearChainButton(
                        text = stringResource(R.string.save),
                        onClick  = { onSubmit(selectedRating, comment.ifBlank { null }) },
                        modifier = Modifier.weight(1f),
                        enabled  = selectedRating > 0,
                        loading = isSubmitting
                    )
                }
            }
        }
    }
}
