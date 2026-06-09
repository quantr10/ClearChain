package com.clearchain.app.presentation.ngo.myrequests

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clearchain.app.domain.model.FoodCategory
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.PickupRequest
import com.clearchain.app.domain.model.PickupRequestStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MyRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    onNavigateToRoute: (String) -> Unit = {},
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showChecklistForId   by remember { mutableStateOf<String?>(null) }
    var showPhotoPickerForId by remember { mutableStateOf<String?>(null) }
    var showFullPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.Navigate -> onNavigateToRoute(event.route)
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


    showFullPhotoUrl?.let { url ->
        FullPhotoDialog(
            photoUrl = url,
            onDismiss = { showFullPhotoUrl = null }
        )
    }

    // Rate & Review dialog
    state.showReviewDialogForId?.let { requestId ->
        ReviewDialog(
            rating          = state.reviewRating,
            comment         = state.reviewComment,
            isSubmitting    = state.isSubmittingReview,
            onRatingChange  = { viewModel.onEvent(MyRequestsEvent.ReviewRatingChanged(it)) },
            onCommentChange = { viewModel.onEvent(MyRequestsEvent.ReviewCommentChanged(it)) },
            onSubmit        = { viewModel.onEvent(MyRequestsEvent.SubmitReview) },
            onDismiss       = { viewModel.onEvent(MyRequestsEvent.DismissReviewDialog) }
        )
    }

    if (state.showFilterSheet) {
        MyRequestsFilterSheet(
            state     = state,
            onEvent   = viewModel::onEvent,
            onDismiss = { viewModel.onEvent(MyRequestsEvent.HideFilterSheet) }
        )
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.isLoading && state.allRequests.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.allRequests.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.msg_failed_load_requests),
                        subtitle = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.onEvent(MyRequestsEvent.LoadRequests) }
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Upload loading banner ──────────────────────────
                        AnimatedVisibility(
                            visible = state.isUploading,
                            enter   = fadeIn(),
                            exit    = fadeOut()
                        ) {
                            AlertBanner(
                                message  = if (state.uploadAttempts > 1)
                                    stringResource(R.string.uploading_photo_attempt, state.uploadAttempts)
                                else stringResource(R.string.uploading_photo),
                                type     = AlertType.INFO,
                                icon     = Icons.Default.CloudUpload
                            )
                        }

                        // ── Upload error banner + retry ────────────────────
                        AnimatedVisibility(
                            visible = state.uploadError != null,
                            enter   = fadeIn(),
                            exit    = fadeOut()
                        ) {
                            Column {
                                AlertBanner(
                                    message = state.uploadError.orEmpty(),
                                    type    = AlertType.ERROR,
                                    icon    = Icons.Default.CloudOff
                                )
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                ) {
                                    TextButton(onClick = { viewModel.onEvent(MyRequestsEvent.DismissUploadError) }) {
                                        Text(stringResource(R.string.close))
                                    }
                                    if (state.uploadAttempts < 3) {
                                        Button(onClick = { viewModel.onEvent(MyRequestsEvent.RetryFailedUpload) }) {
                                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource(R.string.retry))
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchBar(
                                query         = state.searchQuery,
                                onQueryChange = { viewModel.onEvent(MyRequestsEvent.SearchQueryChanged(it)) },
                                placeholder   = stringResource(R.string.hint_search_by_item_grocery),
                                modifier      = Modifier.weight(1f)
                            )
                            BadgedBox(
                                badge = {
                                    if (state.activeFilterCount > 0) Badge { Text(state.activeFilterCount.toString()) }
                                }
                            ) {
                                ClearChainActionIconButton(
                                    icon               = Icons.Default.Tune,
                                    contentDescription = stringResource(R.string.advanced_filters),
                                    onClick            = { viewModel.onEvent(MyRequestsEvent.ShowFilterSheet) }
                                )
                            }
                        }

                        FilterChipsRow(
                            filters = state.availableStatusFilters,
                            selectedFilter = state.selectedStatus,
                            onFilterSelected = { viewModel.onEvent(MyRequestsEvent.StatusFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        ResultsCountAndSort(
                            count = state.filteredRequests.size,
                            itemName = "request",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(MyRequestsEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(MyRequestsEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        when {
                            state.filteredRequests.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allRequests.isEmpty()) Icons.Default.Inbox else Icons.Default.FilterAlt,
                                    title = if (state.allRequests.isEmpty()) stringResource(R.string.empty_no_pickup_requests)
                                    else stringResource(R.string.empty_no_requests_filter),
                                    subtitle = if (state.allRequests.isEmpty())
                                        stringResource(R.string.empty_requests_ngo_subtitle)
                                    else stringResource(R.string.empty_try_filters)
                                )
                            }

                            else -> {
                                HapticPullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh = { viewModel.onEvent(MyRequestsEvent.RefreshRequests) }
                                ) {
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.filteredRequests, key = { it.id }) { request ->
                                            RequestCardWithExtras(
                                                request  = request,
                                                onCardClick     = { onNavigateToRequestDetail(request.id) },
                                                onCancel        = { viewModel.onEvent(MyRequestsEvent.CancelRequest(it)) },
                                                onConfirmPickup = { showChecklistForId = it },
                                                onViewPhoto     = { showFullPhotoUrl = it },
                                                onReview        = { viewModel.onEvent(MyRequestsEvent.ShowReviewDialog(it)) },
                                                onDispute       = { viewModel.onEvent(MyRequestsEvent.DisputeRequest(it)) },
                                                onDownloadReceipt = { viewModel.onEvent(MyRequestsEvent.GenerateReceipt(it)) }
                                            )
                                        }

                                        item { Spacer(Modifier.height(16.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Step 1 — Checklist verification
    showChecklistForId?.let { requestId ->
        PickupChecklistSheet(
            onDismiss = { showChecklistForId = null },
            onNext    = { showChecklistForId = null; showPhotoPickerForId = requestId }
        )
    }

    // Steps 2 & 3 — Photo source + preview
    showPhotoPickerForId?.let { requestId ->
        PhotoPickerDialog(
            onPhotoSelected = { uri ->
                viewModel.onEvent(MyRequestsEvent.ConfirmPickupWithPhoto(requestId, uri))
                showPhotoPickerForId = null
            },
            onDismiss = { showPhotoPickerForId = null }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Advanced filter bottom sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MyRequestsFilterSheet(
    state:     MyRequestsState,
    onEvent:   (MyRequestsEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.advanced_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (state.activeFilterCount > 0) {
                    TextButton(onClick = { onEvent(MyRequestsEvent.ClearAdvancedFilters) }) {
                        Text(stringResource(R.string.action_clear_all))
                    }
                }
            }

            // Food category
            FilterSection(title = stringResource(R.string.filter_category)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.filterCategory == null,
                        onClick  = { onEvent(MyRequestsEvent.FilterCategoryChanged(null)) },
                        label    = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelSmall) }
                    )
                    FoodCategory.entries.forEach { category ->
                        FilterChip(
                            selected = state.filterCategory == category.name,
                            onClick  = {
                                onEvent(MyRequestsEvent.FilterCategoryChanged(
                                    if (state.filterCategory == category.name) null else category.name
                                ))
                            },
                            label    = { Text(stringResource(category.labelResId), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // Pickup date
            FilterSection(title = stringResource(R.string.filter_pickup_date)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        null    to stringResource(R.string.filter_any),
                        "TODAY" to stringResource(R.string.preset_today),
                        "WEEK"  to stringResource(R.string.preset_this_week),
                        "MONTH" to stringResource(R.string.preset_this_month),
                        "PAST"  to stringResource(R.string.filter_past)
                    ).forEach { (preset, label) ->
                        FilterChip(
                            selected = state.filterPickupDatePreset == preset,
                            onClick  = {
                                onEvent(MyRequestsEvent.FilterPickupDatePresetChanged(
                                    if (state.filterPickupDatePreset == preset) null else preset
                                ))
                            },
                            label    = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_apply_filters))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Request card with status timeline + review/dispute actions
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestCardWithExtras(
    request:          PickupRequest,
    onCardClick:      () -> Unit,
    onCancel:         (String) -> Unit,
    onConfirmPickup:  (String) -> Unit,
    onViewPhoto:      (String) -> Unit,
    onReview:         (String) -> Unit,
    onDispute:        (String) -> Unit,
    onDownloadReceipt: (String) -> Unit = {}
) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        RequestCard(
            request         = request,
            modifier        = Modifier.clickable { onCardClick() },
            viewMode        = RequestViewMode.NGO,
            onCancel        = onCancel,
            onConfirmPickup = onConfirmPickup,
            onViewPhoto     = onViewPhoto
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rate & Review dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewDialog(
    rating:          Int,
    comment:         String,
    isSubmitting:    Boolean,
    onRatingChange:  (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit:        () -> Unit,
    onDismiss:       () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(R.string.label_rate_experience)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Star rating row
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { onRatingChange(star) }) {
                            Icon(
                                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.cd_star_n, star),
                                tint   = if (star <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Text(
                    text  = when (rating) {
                        1 -> stringResource(R.string.review_rating_poor)
                        2 -> stringResource(R.string.review_rating_below_avg)
                        3 -> stringResource(R.string.review_rating_average)
                        4 -> stringResource(R.string.review_rating_good)
                        5 -> stringResource(R.string.review_rating_excellent)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                OutlinedTextField(
                    value         = comment,
                    onValueChange = onCommentChange,
                    label         = { Text(stringResource(R.string.label_comments_optional)) },
                    placeholder   = { Text(stringResource(R.string.hint_pickup_experience)) },
                    singleLine    = false,
                    maxLines      = 4,
                    modifier      = Modifier.fillMaxWidth(),
                    enabled       = !isSubmitting
                )
            }
        },
        confirmButton = {
            Button(
                onClick  = onSubmit,
                enabled  = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_submit_review))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text(stringResource(R.string.cancel)) }
        }
    )
}
