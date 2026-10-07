package com.clearchain.app.presentation.shared.requestdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.clearchain.app.R
import com.clearchain.app.data.remote.api.*
import com.clearchain.app.data.remote.dto.*
import com.clearchain.app.data.remote.signalr.SignalRService
import com.clearchain.app.di.ApplicationScope
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestItem
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.domain.usecase.auth.GetCurrentUserUseCase
import com.clearchain.app.domain.usecase.pickuprequest.ConfirmPickupUseCase
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.dispute.NgoDisputeReason
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.ShapeMedium
import com.clearchain.app.util.DateTimeUtils
import com.clearchain.app.util.ImageUtils
import com.clearchain.app.util.PickupReceiptPdf
import com.clearchain.app.util.UiEvent
import com.clearchain.app.util.dialPhone
import com.clearchain.app.util.mapsQuery
import com.clearchain.app.util.openInGoogleMaps
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val PICKUP_CHECKLIST_SIZE = 5

// -- State --
data class RequestDetailState(
    val request: PickupRequest? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserId: String? = null,
    val currentUserType: OrganizationType? = null,
    val isActionLoading: Boolean = false,
    val showRejectDialog: Boolean = false,
    val checkedItems: Set<Int> = emptySet(),
    val messages: List<MessageData> = emptyList(),
    val messageInput: String = "",
    val isSendingMessage: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val myReview: ReviewData? = null,
    val ngoReview: ReviewData? = null,
    val isSubmittingReview: Boolean = false,
    val showAutoRatingSheet: Boolean = false,
    val showRatingSheet: Boolean = false,
    val isGeneratingReceipt: Boolean = false,
    val groceryProfile: PublicProfileData? = null,
    // NGO dispute on this pickup. The button stays hidden until disputeChecked, so a slow or failed
    // lookup can't invite a second report.
    val myDispute: MyDisputeData? = null,
    val disputeChecked: Boolean = false,
    val showDisputeSheet: Boolean = false,
    val isSubmittingDispute: Boolean = false,
    // Admin's view of the dispute on this pickup, plus the actions that move it along.
    val adminDispute: DisputeListItemData? = null,
    val isStartingReview: Boolean = false,
    val showResolveDialog: Boolean = false,
    val resolveOutcome: String = "resolved_ngo",
    val resolveGroceryStatement: String = "",
    val resolveNote: String = "",
    val isResolving: Boolean = false
) {
    val allChecked: Boolean get() = checkedItems.size == PICKUP_CHECKLIST_SIZE
}

// -- ViewModel --
@HiltViewModel
class RequestDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pickupRequestApi: PickupRequestApi,
    private val messageApi: MessageApi,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val confirmPickupUseCase: ConfirmPickupUseCase,
    private val reviewApi: ReviewApi,
    private val organizationApi: OrganizationApi,
    private val disputeApi: DisputeApi,
    private val signalRService: SignalRService,
    @ApplicationScope private val applicationScope: CoroutineScope
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
        joinedRequestId?.let { requestId ->
            applicationScope.launch {
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
                // The user is read from storage in init, so on a fast response it may not be in the
                // state yet; wait for it rather than skipping the role-specific loads.
                val userType = _state.value.currentUserType
                    ?: getCurrentUserUseCase().first()?.type?.also { type ->
                        _state.update { it.copy(currentUserType = type) }
                    }
                if (userType == OrganizationType.NGO) {
                    loadGroceryProfile(req.groceryId)
                    loadMyDispute(requestId)
                }
                if (userType == OrganizationType.ADMIN) {
                    loadAdminDispute(requestId)
                }
                if (req.status == PickupRequestStatus.COMPLETED) {
                    if (userType == OrganizationType.NGO) {
                        loadMyReview(requestId)
                    } else if (userType == OrganizationType.GROCERY || userType == OrganizationType.ADMIN) {
                        loadNgoReview(requestId, req.ngoId)
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: context.getString(R.string.error_failed_load_request), isLoading = false) }
            }
        }
    }

    fun loadMyReview(requestId: String) {
        viewModelScope.launch {
            try {
                // Read in init, so on a fast response it may not be in the state yet.
                val me = _state.value.currentUserId ?: getCurrentUserUseCase().first()?.id
                val mine = reviewApi.getReviewsForPickup(requestId).data.find { it.reviewerId == me }
                _state.update { it.copy(myReview = mine) }

                // Only evaluate the auto-sheet once per ViewModel instance.
                // autoSheetShownFor acts as an in-session guard so coroutine races
                // can never trigger the sheet twice even if loadMyReview is called again.
                if (autoSheetShownFor != requestId) {
                    autoSheetShownFor = requestId
                    val prefs = context.getSharedPreferences("request_detail_prefs", Context.MODE_PRIVATE)
                    val seenKey = "seen_complete_$requestId"
                    if (!prefs.getBoolean(seenKey, false)) {
                        // commit() writes synchronously so the flag survives rapid ViewModel recreation.
                        prefs.edit(commit = true) { putBoolean(seenKey, true) }
                        _state.update { it.copy(showAutoRatingSheet = true) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /** The rating the NGO gave this pickup, for the grocery that received it and for an admin. */
    private fun loadNgoReview(requestId: String, ngoId: String) {
        viewModelScope.launch {
            try {
                val response = reviewApi.getReviewsForPickup(requestId)
                // A pickup can carry a review from each side; this is the one the NGO wrote.
                val review = response.data.find { it.reviewerId == ngoId }
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
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_submit_review_failed)))
            }
        }
    }

    fun dismissAutoRatingSheet() = _state.update { it.copy(showAutoRatingSheet = false) }
    fun openRatingSheet() = _state.update { it.copy(showRatingSheet = true) }
    fun closeRatingSheet() = _state.update { it.copy(showRatingSheet = false) }

    fun generateReceipt() {
        val req = _state.value.request ?: return
        viewModelScope.launch {
            _state.update { it.copy(isGeneratingReceipt = true) }
            try {
                val uri = withContext(Dispatchers.IO) { PickupReceiptPdf.build(context, req) }
                _uiEvent.send(UiEvent.ShareFile(uri, title = context.getString(R.string.snack_receipt_title, req.listingTitle)))
            } catch (e: Exception) {
                // Logged because the snackbar alone hid why this failed for a long time.
                Log.e("RequestDetail", "Could not generate the pickup receipt", e)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_receipt_failed)))
            } finally {
                _state.update { it.copy(isGeneratingReceipt = false) }
            }
        }
    }

    private fun loadMyDispute(requestId: String) {
        viewModelScope.launch {
            runCatching { disputeApi.getMyDisputes() }
                .onSuccess { response ->
                    val mine = response.data.find { it.pickupRequestId == requestId }
                    _state.update { it.copy(myDispute = mine, disputeChecked = true) }
                }
        }
    }

    private fun loadAdminDispute(requestId: String) {
        viewModelScope.launch {
            runCatching { disputeApi.getDisputes(pickupRequestId = requestId) }
                .onSuccess { response ->
                    _state.update { it.copy(adminDispute = response.data.firstOrNull()) }
                }
        }
    }

    fun startDisputeReview(requestId: String) {
        val dispute = _state.value.adminDispute ?: return
        if (_state.value.isStartingReview) return
        viewModelScope.launch {
            _state.update { it.copy(isStartingReview = true) }
            try {
                disputeApi.startReview(dispute.id)
                _state.update { it.copy(isStartingReview = false) }
                loadAdminDispute(requestId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dispute_review_started)))
            } catch (e: Exception) {
                _state.update { it.copy(isStartingReview = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_start_review_failed)))
            }
        }
    }

    fun openResolveDialog() = _state.update {
        it.copy(showResolveDialog = true, resolveOutcome = "resolved_ngo", resolveGroceryStatement = "", resolveNote = "")
    }
    fun closeResolveDialog() = _state.update { it.copy(showResolveDialog = false) }
    fun onResolveOutcomeChange(value: String) = _state.update { it.copy(resolveOutcome = value) }
    fun onResolveGroceryStatementChange(value: String) = _state.update { it.copy(resolveGroceryStatement = value) }
    fun onResolveNoteChange(value: String) = _state.update { it.copy(resolveNote = value) }

    fun resolveDispute(requestId: String) {
        val dispute = _state.value.adminDispute ?: return
        val note = _state.value.resolveNote.trim()
        if (note.isBlank()) {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.error_resolution_note_required)))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isResolving = true) }
            try {
                disputeApi.resolveDispute(
                    dispute.id,
                    ResolveDisputeRequest(
                        status = _state.value.resolveOutcome,
                        groceryStatement = _state.value.resolveGroceryStatement.trim().ifBlank { null },
                        adminResolution = note
                    )
                )
                _state.update { it.copy(showResolveDialog = false, isResolving = false) }
                loadAdminDispute(requestId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dispute_resolved)))
            } catch (e: Exception) {
                _state.update { it.copy(isResolving = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: context.getString(R.string.error_resolve_dispute_failed)))
            }
        }
    }

    fun openDisputeSheet() = _state.update { it.copy(showDisputeSheet = true) }
    fun closeDisputeSheet() = _state.update { it.copy(showDisputeSheet = false) }

    /**
     * Files the NGO's dispute. On failure the sheet stays open so the typed statement and photo
     * are not lost; on success the sheet closes and the pickup shows the new dispute.
     */
    fun submitDispute(requestId: String, reason: NgoDisputeReason, statement: String, photoUri: Uri?) {
        if (_state.value.isSubmittingDispute) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingDispute = true) }
            var photoFile: File? = null
            try {
                photoFile = photoUri?.let { withContext(Dispatchers.IO) { ImageUtils.compressImage(context, it) } }
                val photoPart = photoFile?.let {
                    MultipartBody.Part.createFormData("photo", it.name, it.asRequestBody("image/jpeg".toMediaTypeOrNull()))
                }
                disputeApi.openDispute(
                    pickupRequestId = requestId.toRequestBody(),
                    reason = reason.key.toRequestBody(),
                    statement = statement.toRequestBody(),
                    photo = photoPart
                )
                _state.update { it.copy(isSubmittingDispute = false, showDisputeSheet = false) }
                loadMyDispute(requestId)
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.snack_dispute_submitted)))
            } catch (_: Exception) {
                _state.update { it.copy(isSubmittingDispute = false) }
                _uiEvent.send(UiEvent.ShowSnackbar(context.getString(R.string.error_submit_dispute_failed)))
            } finally {
                // The compressed copy is only a temporary upload file.
                photoFile?.delete()
            }
        }
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
            // Same endpoint as cancel() — the backend tells reject and cancel apart by
            // whether the caller is the request's grocery or its NGO.
            pickupRequestApi.cancelPickupRequest(requestId)
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

    fun showRejectDialog() = _state.update { it.copy(showRejectDialog = true) }
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
            _state.update { it.copy(isActionLoading = true) }
            try {
                block()
                loadRequest(requestId)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.ShowSnackbar(e.message ?: "Action failed"))
            } finally {
                _state.update { it.copy(isActionLoading = false) }
            }
        }
    }
}

// -- Screen --
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(
    requestId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit = {},
    viewModel: RequestDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showChatSheet by remember { mutableStateOf(false) }

    val req = state.request
    val isGrocery = state.currentUserType == OrganizationType.GROCERY
    val isNgo = state.currentUserType == OrganizationType.NGO
    val isAdmin = state.currentUserType == OrganizationType.ADMIN
    val isMyRequest = req != null && when {
        isGrocery -> req.groceryId == state.currentUserId
        isNgo -> req.ngoId == state.currentUserId
        else -> false
    }
    val isChatVisible = isMyRequest &&
        req.status != PickupRequestStatus.COMPLETED &&
        req.status != PickupRequestStatus.CANCELLED &&
        req.status != PickupRequestStatus.REJECTED

    var showChecklistSheet by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }

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
    if (state.showAutoRatingSheet && req != null && isNgo && state.myReview == null) {
        RatingDialog(
            isSubmitting = state.isSubmittingReview,
            onDismiss = { viewModel.dismissAutoRatingSheet() },
            onSubmit = { rating, comment -> viewModel.submitReview(requestId, rating, comment) }
        )
    }

    // Manual rating sheet from "Show" button (NGO only)
    if (state.showRatingSheet && req != null && isNgo && state.myReview == null) {
        RatingDialog(
            isSubmitting = state.isSubmittingReview,
            onDismiss = { viewModel.closeRatingSheet() },
            onSubmit = { rating, comment -> viewModel.submitReview(requestId, rating, comment) }
        )
    }

    // Step 1 - Checklist verification sheet
    if (showChecklistSheet) {
        PickupChecklistDialog(
            onDismiss = { showChecklistSheet = false },
            onNext = {
                showChecklistSheet = false
                showPhotoPicker = true
            }
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

    // Admin: resolve the dispute on this pickup
    if (state.showResolveDialog) {
        ResolveDisputeDialog(
            outcome = state.resolveOutcome,
            groceryStatement = state.resolveGroceryStatement,
            note = state.resolveNote,
            isResolving = state.isResolving,
            onOutcomeChange = viewModel::onResolveOutcomeChange,
            onGroceryStatementChange = viewModel::onResolveGroceryStatementChange,
            onNoteChange = viewModel::onResolveNoteChange,
            onConfirm = { viewModel.resolveDispute(requestId) },
            onDismiss = { viewModel.closeResolveDialog() }
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                ChatSection(
                    messages = state.messages,
                    currentUserId = state.currentUserId ?: "",
                    messageInput = state.messageInput,
                    isSending = state.isSendingMessage,
                    isLoading = state.isLoadingMessages,
                    onInputChanged = { viewModel.onMessageInputChanged(it) },
                    onSend = { viewModel.sendMessage(requestId) }
                )
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isChatVisible) {
                SmallFloatingActionButton(
                    onClick = {
                        showChatSheet = true
                        viewModel.loadMessages(requestId)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = stringResource(R.string.label_messages_section))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.error_generic),
                        subtitle = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.loadRequest(requestId) }
                    )
                    req != null -> RequestDetailContent(
                        req = req,
                        state = state,
                        isGrocery = isGrocery,
                        isNgo = isNgo,
                        isAdmin = isAdmin,
                        isMyRequest = isMyRequest,
                        // Only the NGO that owns a completed pickup can dispute it, and only once; after
                        // that the same button opens the submitted dispute read-only.
                        showDisputeButton = isNgo && isMyRequest &&
                            state.disputeChecked &&
                            req.status == PickupRequestStatus.COMPLETED,
                        onOpenDispute = { viewModel.openDisputeSheet() },
                        onCloseDispute = { viewModel.closeDisputeSheet() },
                        onSubmitDispute = { reason, statement, photo ->
                            viewModel.submitDispute(requestId, reason, statement, photo)
                        },
                        onStartDisputeReview = { viewModel.startDisputeReview(requestId) },
                        onOpenResolveDialog = { viewModel.openResolveDialog() },
                        onApprove = { viewModel.approve(requestId) },
                        onReject = { viewModel.showRejectDialog() },
                        onMarkReady = { viewModel.markReady(requestId) },
                        onCancel = { viewModel.cancel(requestId) },
                        onConfirmPickup = { showChecklistSheet = true },
                        onNavigateToPublicProfile = onNavigateToPublicProfile,
                        onGenerateReceipt = { viewModel.generateReceipt() },
                        onShowRatingSheet = { viewModel.openRatingSheet() },
                        groceryProfile = state.groceryProfile
                    )
                }
            }
        }
    }
}

// -- Main content --
@Composable
private fun RequestDetailContent(
    req: PickupRequest,
    state: RequestDetailState,
    isGrocery: Boolean,
    isNgo: Boolean,
    isAdmin: Boolean,
    isMyRequest: Boolean,
    showDisputeButton: Boolean,
    onOpenDispute: () -> Unit,
    onCloseDispute: () -> Unit,
    onSubmitDispute: (NgoDisputeReason, String, Uri?) -> Unit,
    onStartDisputeReview: () -> Unit,
    onOpenResolveDialog: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMarkReady: () -> Unit,
    onCancel: () -> Unit,
    onConfirmPickup: () -> Unit,
    onNavigateToPublicProfile: (String) -> Unit,
    onGenerateReceipt: () -> Unit,
    onShowRatingSheet: () -> Unit,
    groceryProfile: PublicProfileData? = null
) {
    val context = LocalContext.current
    // -- Expiry computation (same logic as RequestCard) --
    val daysUntilExpiry: Long? = remember(req.listingExpiryDate) {
        val raw = req.listingExpiryDate ?: return@remember null
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(raw)!!
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            TimeUnit.MILLISECONDS.toDays(date.time - today.time)
        } catch (_: Exception) {
            null
        }
    }
    val expiryColor = daysUntilExpiry?.let {
        when {
            it <= 0L -> MaterialTheme.colorScheme.error
            it <= 3L -> Color(0xFFE65100)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    } ?: MaterialTheme.colorScheme.onSurfaceVariant

    val expiryText: String? = daysUntilExpiry?.let {
        when {
            it < 0 -> stringResource(R.string.listing_expired_label)
            it == 0L -> stringResource(R.string.listing_expires_today)
            it == 1L -> stringResource(R.string.listing_expires_tomorrow)
            it in 2..3 -> stringResource(R.string.listing_expires_in_days, it.toInt())
            else -> stringResource(R.string.listing_expires_on, DateTimeUtils.formatDate(req.listingExpiryDate!!))
        }
    }

    // -- Pickup timestamp --
    val timestampText = stringResource(
        R.string.label_pickup_on_at,
        DateTimeUtils.formatDate(req.pickupDate),
        req.pickupTime
    )

    val handlingParts = buildList {
        if (req.requiresRefrigeration) add(stringResource(R.string.note_needs_refrigeration))
        if (req.isFragile) add(stringResource(R.string.note_fragile_items))
        if (req.isHeavy) add(stringResource(R.string.note_heavy_load))
        req.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // -- 1. Avatar + party name card (with action buttons top-right) --
        val showReceiptBtn = isMyRequest && req.status == PickupRequestStatus.COMPLETED
        val partyName = if (isGrocery) req.ngoName else req.groceryName
        val partyType = if (isGrocery) {
            stringResource(R.string.label_ngo_party)
        } else {
            stringResource(R.string.label_grocery_party)
        }
        val partyId = if (isGrocery) req.ngoId else req.groceryId
        val partyAvatar = if (isGrocery) {
            req.ngoProfilePictureUrl
        } else {
            req.groceryProfilePictureUrl
        }

        if (isAdmin) {
            // An admin is neither party, so show both with their contact details.
            ClearChainCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PartyContactRow(
                        role = stringResource(R.string.label_ngo_party),
                        name = req.ngoName,
                        avatarUrl = req.ngoProfilePictureUrl,
                        email = req.ngoEmail,
                        phone = req.ngoPhone,
                        onOpenProfile = { onNavigateToPublicProfile(req.ngoId) }
                    )
                    PartyContactRow(
                        role = stringResource(R.string.label_grocery_party),
                        name = req.groceryName,
                        avatarUrl = req.groceryProfilePictureUrl,
                        email = req.groceryEmail,
                        phone = req.groceryPhone,
                        onOpenProfile = { onNavigateToPublicProfile(req.groceryId) }
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(148.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                ) {
                    if (showReceiptBtn) {
                        Row(
                            modifier = Modifier.align(Alignment.TopEnd),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                        ) {
                            if (showReceiptBtn) {
                                ImageActionButton(
                                    icon = Icons.Default.Receipt,
                                    label = stringResource(R.string.cd_download_receipt),
                                    loading = state.isGeneratingReceipt,
                                    onClick = onGenerateReceipt
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onNavigateToPublicProfile(partyId) },
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (!partyAvatar.isNullOrBlank()) {
                                AsyncImage(
                                    model = partyAvatar,
                                    contentDescription = partyName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        partyName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Text(partyName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(partyType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (state.showDisputeSheet && state.myDispute == null) {
            DisputeDialog(
                isSubmitting = state.isSubmittingDispute,
                onDismiss = onCloseDispute,
                onSubmit = onSubmitDispute
            )
        }

        // -- 2. Lifecycle timeline (already a Card) --
        LifecycleTimeline(request = req)

        // -- 4. Description card --
        if (!req.listingDescription.isNullOrBlank()) {
            SectionCard(stringResource(R.string.label_description)) {
                Text(
                    req.listingDescription.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // -- 5. Request Info card --
        if (req.items.isNotEmpty()) {
            SectionCard(
                title = stringResource(R.string.cart_requested_items),
                action = {
                    Text(
                        "${req.items.size} ${if (req.items.size == 1) "item" else "items"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            ) {
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
                CompactDetailRow(Icons.Default.CalendarToday, expiryText, textColor = expiryColor)
            }
            CompactDetailRow(Icons.Default.AccessTime, timestampText, textColor = MaterialTheme.colorScheme.onSurfaceVariant)
            if (handlingParts.isNotEmpty()) {
                CompactDetailRow(Icons.AutoMirrored.Filled.StickyNote2, handlingParts.joinToString(" \u00B7 "), textColor = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Admin: the dispute on this pickup, right after the request details it refers to.
        state.adminDispute?.let { dispute ->
            AdminDisputeSection(
                dispute = dispute,
                isStartingReview = state.isStartingReview,
                onStartReview = onStartDisputeReview,
                onResolve = onOpenResolveDialog
            )
        }

        // -- 5b. About Us card (NGO only) --
        if (isNgo && groceryProfile != null) {
            // Street (up to the first comma, in case the stored address already includes
            // the city) + city + state + ZIP, on one line - matches Location & Hours.
            val address = listOfNotNull(
                groceryProfile.address?.substringBefore(',')?.trim()?.takeIf { it.isNotBlank() },
                groceryProfile.location?.trim()?.takeIf { it.isNotBlank() },
                groceryProfile.state?.trim()?.takeIf { it.isNotBlank() },
                groceryProfile.zipCode?.trim()?.takeIf { it.isNotBlank() }
            ).joinToString(", ").takeIf { it.isNotBlank() }

            SectionCard(stringResource(R.string.section_about_us)) {
                if (address != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Place,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            address,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        ClearChainActionIconButton(
                            icon = Icons.Default.Navigation,
                            contentDescription = stringResource(R.string.action_get_directions),
                            onClick = {
                                openInGoogleMaps(
                                    context,
                                    mapsQuery(groceryProfile.latitude, groceryProfile.longitude, address)
                                )
                            }
                        )
                    }
                }

                groceryProfile.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                    Row(
                        modifier = Modifier.clickable { dialPhone(context, phone) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            null,
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

        // -- 7. Action buttons --
        if (isMyRequest) {
            if (isNgo && req.status == PickupRequestStatus.READY) {
                ClearChainButton(
                    text = stringResource(R.string.action_confirm_pickup_photo),
                    onClick = onConfirmPickup,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PhotoCamera,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }

            if (isGrocery && req.status == PickupRequestStatus.PENDING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ClearChainOutlinedButton(
                        text = stringResource(R.string.reject),
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Close,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                    ClearChainButton(
                        text = stringResource(R.string.approve),
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Check
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

        // -- 8. Rating card (NGO: submit rating; Grocery and admin: view the received rating) --
        if (req.status == PickupRequestStatus.COMPLETED && isMyRequest && isNgo) {
            val myReview = state.myReview
            if (myReview != null) {
                // Already rated: the rating itself is the section, nothing to open.
                ReviewSection(
                    title = stringResource(R.string.your_rating),
                    rating = myReview.rating,
                    comment = myReview.comment,
                    createdAt = myReview.createdAt
                )
            } else {
                SectionCard(stringResource(R.string.label_rate_experience)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onShowRatingSheet),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.hint_pickup_experience),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        }

        // The NGO's rating of the grocery. The grocery sees it, or that it is still missing; an admin
        // sees it only once it exists, so a pickup nobody rated shows no rating section at all.
        if (req.status == PickupRequestStatus.COMPLETED && ((isMyRequest && isGrocery) || isAdmin)) {
            val review = state.ngoReview
            if (review != null) {
                ReviewSection(
                    title = stringResource(R.string.label_ngo_rating),
                    rating = review.rating,
                    comment = review.comment,
                    createdAt = review.createdAt
                )
            } else if (isGrocery) {
                SectionCard(stringResource(R.string.label_ngo_rating)) {
                    NoReviewYet(stringResource(R.string.hint_not_yet_rated))
                }
            }
        }

        // -- 8b. Dispute (NGO): the report already filed, or the prompt to file one --
        if (showDisputeButton) {
            val myDispute = state.myDispute
            if (myDispute != null) {
                MyDisputeSection(myDispute)
            } else {
                SectionCard(stringResource(R.string.open_dispute)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenDispute),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.hint_dispute_report),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(12.dp))
                        ClearChainActionIconButton(
                            icon = Icons.Default.Flag,
                            contentDescription = stringResource(R.string.action_file_dispute),
                            onClick = onOpenDispute
                        )
                    }
                }
            }
        }

        // -- 9. Proof Photo card (completed) --
        if (req.status == PickupRequestStatus.COMPLETED && req.proofPhotoUrl != null) {
            SectionCard(stringResource(R.string.proof_photo)) {
                ZoomablePhoto(url = req.proofPhotoUrl)
            }
        }
    }
}

// -- Compact section label --
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
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = quantityText,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

// -- Zoomable proof photo --
@Composable
private fun ZoomablePhoto(url: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { _, zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
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
            model = url,
            contentDescription = stringResource(R.string.cd_proof_of_pickup),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Crop
        )
    }
}

// -- Lifecycle Timeline --
@Composable
private fun LifecycleTimeline(request: PickupRequest) {
    val icons = listOf(
        Icons.AutoMirrored.Filled.Send,
        Icons.Default.CheckCircle,
        Icons.Default.Inventory2,
        Icons.Default.TaskAlt
    )

    val currentIndex = when (request.status) {
        PickupRequestStatus.PENDING -> 0
        PickupRequestStatus.APPROVED -> 1
        PickupRequestStatus.READY -> 2
        PickupRequestStatus.COMPLETED -> 3
        PickupRequestStatus.CANCELLED,
        PickupRequestStatus.REJECTED -> -1
    }

    val green = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.outlineVariant
    val isNegativeStatus = request.status == PickupRequestStatus.CANCELLED ||
        request.status == PickupRequestStatus.REJECTED

    // -- Current-stage text --
    val statusTitle = when (request.status) {
        PickupRequestStatus.PENDING -> stringResource(R.string.label_status_submitted)
        PickupRequestStatus.APPROVED -> stringResource(R.string.label_status_approved)
        PickupRequestStatus.READY -> stringResource(R.string.label_status_ready)
        PickupRequestStatus.COMPLETED -> stringResource(R.string.label_status_completed)
        PickupRequestStatus.CANCELLED -> stringResource(R.string.label_status_cancelled)
        PickupRequestStatus.REJECTED -> stringResource(R.string.label_status_rejected)
    }
    val statusSub = when (request.status) {
        PickupRequestStatus.PENDING -> stringResource(
            R.string.label_requested_on_at,
            DateTimeUtils.formatDate(request.createdAt),
            DateTimeUtils.formatTime(request.createdAt)
        )
        PickupRequestStatus.APPROVED -> stringResource(
            R.string.label_approved_on,
            DateTimeUtils.formatDate(request.pickupDate),
            request.pickupTime
        )
        PickupRequestStatus.READY -> stringResource(
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
        PickupRequestStatus.CANCELLED -> stringResource(
            R.string.label_cancelled_on,
            DateTimeUtils.formatDateTime(request.createdAt)
        )
        PickupRequestStatus.REJECTED -> stringResource(
            R.string.label_rejected_on,
            DateTimeUtils.formatDateTime(request.createdAt)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status sentence block
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    statusTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isNegativeStatus) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    statusSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isNegativeStatus) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Horizontal icon bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icons.forEachIndexed { index, icon ->
                    val isReached = currentIndex >= 0 && index <= currentIndex
                    val iconTint = if (isReached) green else muted
                    val iconBg = if (isReached) green.copy(alpha = 0.15f) else muted.copy(alpha = 0.15f)

                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = iconBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = iconTint
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
}

// -- Chat Section --
@Composable
private fun ChatSection(
    messages: List<MessageData>,
    currentUserId: String,
    messageInput: String,
    isSending: Boolean,
    isLoading: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    val inputInteractionSource = remember { MutableInteractionSource() }
    val inputFocused by inputInteractionSource.collectIsFocusedAsState()
    val inputBorderColor = if (inputFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())

        if (messages.isEmpty() && !isLoading) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.msg_no_messages),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(message = msg, isMine = msg.senderId == currentUserId)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
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
                color = if (messageInput.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
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
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
        if (!isMine) {
            Text(
                message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isMine) 16.dp else 4.dp,
                topEnd = if (isMine) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            color = bubbleColor,
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
            DateTimeUtils.getTimeAgo(LocalContext.current, message.sentAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, start = 8.dp, end = 8.dp)
        )
    }
}

// -- Dispute dialog --
@Composable
private fun DisputeDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (NgoDisputeReason, String, Uri?) -> Unit
) {
    var selectedReason by remember { mutableStateOf<NgoDisputeReason?>(null) }
    var statement by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
    }

    // The description is required, matching the server's check.
    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.Flag,
        title = stringResource(R.string.open_dispute),
        message = stringResource(R.string.msg_dispute_report),
        confirmLabel = stringResource(R.string.dispute_submit),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = selectedReason != null && statement.isNotBlank() && !isSubmitting,
        confirmLoading = isSubmitting,
        dismissEnabled = !isSubmitting,
        dismissible = !isSubmitting,
        onConfirm = { selectedReason?.let { onSubmit(it, statement.trim(), photoUri) } }
    ) {
        // The form is taller than a dialog on a small screen, so it scrolls inside the dialog.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Same label style as the text field titles below it (ClearChainTextField).
            Text(
                stringResource(R.string.dispute_reason),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            // The radio's 48dp minimum touch target is switched off here so the rows can sit close together;
            // the whole row stays clickable.
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Column {
                    NgoDisputeReason.entries.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReason = reason }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason }
                            )
                            Text(stringResource(reason.labelRes), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            ClearChainTextField(
                value = statement,
                onValueChange = { statement = it },
                label = stringResource(R.string.dispute_statement_label),
                placeholder = stringResource(R.string.dispute_statement_hint),
                singleLine = false,
                minLines = 3,
                maxLines = 6,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth()
            )

            // Photo evidence (optional)
            val photo = photoUri
            if (photo == null) {
                ClearChainOutlinedButton(
                    text = stringResource(R.string.dispute_photo),
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Image
                )
            } else {
                Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))) {
                    AsyncImage(
                        model = photo,
                        contentDescription = stringResource(R.string.dispute_photo_evidence),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = { photoUri = null }, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

// -- Rating dialog --
@Composable
private fun RatingDialog(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit
) {
    var selectedRating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.StarRate,
        title = stringResource(R.string.label_rate_experience),
        message = stringResource(R.string.msg_rate_experience),
        confirmLabel = stringResource(R.string.save),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = selectedRating > 0 && !isSubmitting,
        confirmLoading = isSubmitting,
        dismissEnabled = !isSubmitting,
        dismissible = !isSubmitting,
        onConfirm = { onSubmit(selectedRating, comment.ifBlank { null }) }
    ) {
        Text(
            stringResource(R.string.rate_and_review),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(5) { i ->
                IconButton(
                    onClick = { selectedRating = i + 1 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (i < selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = pluralStringResource(R.plurals.cd_star_n, i + 1, i + 1),
                        modifier = Modifier.size(32.dp),
                        tint = if (i < selectedRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
        ClearChainTextField(
            value = comment,
            onValueChange = { comment = it },
            label = stringResource(R.string.label_comments_optional),
            isOptional = true,
            placeholder = stringResource(R.string.hint_pickup_experience),
            singleLine = false,
            minLines = 2,
            maxLines = 4,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
