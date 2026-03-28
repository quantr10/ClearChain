// ═══════════════════════════════════════════════════════════════════════════════
// ManageRequestsScreen.kt — Split filters (no FilterSection)
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery.managerequests

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRequestsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},
    viewModel: ManageRequestsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Requests") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(ManageRequestsEvent.RefreshRequests) }
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
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
                        onAction = { viewModel.onEvent(ManageRequestsEvent.LoadRequests) }
                    )
                }

                // ── 3. Normal → filters always visible + content ────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Search bar
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(ManageRequestsEvent.SearchQueryChanged(it)) },
                            placeholder = "Search by item, NGO...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // Status chips
                        FilterChipsRow(
                            filters = state.availableStatusFilters,
                            selectedFilter = state.selectedStatus,
                            onFilterSelected = { viewModel.onEvent(ManageRequestsEvent.StatusFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Results count + Sort
                        ResultsCountAndSort(
                            count = state.filteredRequests.size,
                            itemName = "request",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(ManageRequestsEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Inline error (after data loaded but action failed)
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(ManageRequestsEvent.ClearError) },
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
                                        "Requests from NGOs will appear here"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
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
                                            viewMode = RequestViewMode.GROCERY,
                                            onApprove = { viewModel.onEvent(ManageRequestsEvent.ApproveRequest(it)) },
                                            onReject = { viewModel.onEvent(ManageRequestsEvent.RejectRequest(it)) },
                                            onMarkReady = { viewModel.onEvent(ManageRequestsEvent.MarkReady(it)) }
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