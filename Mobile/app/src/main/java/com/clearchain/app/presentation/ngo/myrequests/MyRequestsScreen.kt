// ═══════════════════════════════════════════════════════════════════════════════
// MyRequestsScreen.kt — Split filters, same pattern as all list screens
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.myrequests

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showPhotoPickerForId by remember { mutableStateOf<String?>(null) }
    var showFullPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    // ── Upload Overlay ──────────────────────────────────────────────
    if (state.isUploading) {
        LoadingOverlay(
            message = if (state.uploadAttempts > 1)
                "Uploading photo... (Attempt ${state.uploadAttempts}/3)"
            else "Uploading photo..."
        )
    }

    // ── Upload Error Dialog ─────────────────────────────────────────
    state.uploadError?.let {
        UploadErrorDialog(
            errorMessage = it,
            canRetry = state.uploadAttempts < 3,
            onRetry = { viewModel.onEvent(MyRequestsEvent.RetryFailedUpload) },
            onDismiss = { viewModel.onEvent(MyRequestsEvent.DismissUploadError) }
        )
    }

    // ── Full Photo Viewer ───────────────────────────────────────────
    showFullPhotoUrl?.let { url ->
        FullPhotoDialog(
            photoUrl = url,
            onDismiss = { showFullPhotoUrl = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                // ── 1. First load → fullscreen spinner ──────────────
                state.isLoading && state.allRequests.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // ── 2. First load error → fullscreen error + Retry ──
                state.error != null && state.allRequests.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Failed to load requests",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(MyRequestsEvent.LoadRequests) }
                    )
                }

                // ── 3. Normal → filters always visible + content ────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Search bar
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(MyRequestsEvent.SearchQueryChanged(it)) },
                            placeholder = "Search by item, grocery...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // Status chips
                        FilterChipsRow(
                            filters = state.availableStatusFilters,
                            selectedFilter = state.selectedStatus,
                            onFilterSelected = { viewModel.onEvent(MyRequestsEvent.StatusFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Results count + Sort
                        ResultsCountAndSort(
                            count = state.filteredRequests.size,
                            itemName = "request",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(MyRequestsEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Inline error
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(MyRequestsEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Content area
                        when {
                            state.filteredRequests.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allRequests.isEmpty()) Icons.Default.Inbox else Icons.Default.FilterAlt,
                                    title = if (state.allRequests.isEmpty()) "No pickup requests yet"
                                    else "No requests match your filters",
                                    subtitle = if (state.allRequests.isEmpty())
                                        "Browse food listings to make your first request"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
                                PullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh = { viewModel.onEvent(MyRequestsEvent.RefreshRequests) }
                                ) {                                
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(state.filteredRequests, key = { it.id }) { request ->
                                            RequestCard(
                                                request = request,
                                                modifier = Modifier.clickable {
                                                    onNavigateToRequestDetail(request.id)
                                                },
                                                viewMode = RequestViewMode.NGO,
                                                onCancel = { viewModel.onEvent(MyRequestsEvent.CancelRequest(it)) },
                                                onConfirmPickup = { showPhotoPickerForId = it },
                                                onViewPhoto = { showFullPhotoUrl = it }
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

    // ── Photo Picker Dialog ─────────────────────────────────────────
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