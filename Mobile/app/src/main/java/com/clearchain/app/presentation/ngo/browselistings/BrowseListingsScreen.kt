// ═══════════════════════════════════════════════════════════════════════════════
// BrowseListingsScreen.kt — Same pattern as all other list screens
// Fullscreen error on first load fail, filters always visible
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.ngo.browselistings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseListingsScreen(
    navController: NavController,
    viewModel: BrowseListingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> navController.navigate(event.route)
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Browse Food") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(BrowseListingsEvent.RefreshListings) },
                        enabled = !state.isRefreshing
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // ── 1. First load → fullscreen spinner ──────────────
                state.isLoading && state.allListings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                // ── 2. First load error → fullscreen error + Retry ──
                state.error != null && state.allListings.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Something went wrong",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(BrowseListingsEvent.LoadListings) }
                    )
                }

                // ── 3. Normal → filters always visible + content ────
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Filters
                        FilterSection(
                            searchQuery = state.searchQuery,
                            onSearchQueryChange = {
                                viewModel.onEvent(BrowseListingsEvent.SearchQueryChanged(it))
                            },
                            searchPlaceholder = "Search by name, grocery, location...",
                            selectedSort = state.selectedSort,
                            onSortSelected = {
                                viewModel.onEvent(BrowseListingsEvent.SortOptionChanged(it))
                            },
                            sortOptions = state.availableSortOptions,
                            filterChips = state.availableCategoryFilters,
                            selectedFilter = state.selectedCategory,
                            onFilterSelected = {
                                viewModel.onEvent(BrowseListingsEvent.CategoryFilterChanged(it))
                            },
                            resultsCount = state.filteredListings.size,
                            itemName = "listing"
                        )

                        // Inline error (after data loaded but action failed)
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(BrowseListingsEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        // Content area
                        when {
                            state.filteredListings.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allListings.isEmpty()) Icons.Default.SearchOff else Icons.Default.FilterAlt,
                                    title = if (state.allListings.isEmpty()) "No available listings"
                                    else "No listings match your filters",
                                    subtitle = if (state.allListings.isEmpty())
                                        "Check back later for new surplus food"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(
                                        items = state.filteredListings,
                                        key = { it.id }
                                    ) { listing ->
                                        ListingCard(
                                            listing = listing,
                                            showGroceryInfo = true,
                                            primaryAction = {
                                                Button(
                                                    onClick = {
                                                        viewModel.onEvent(
                                                            BrowseListingsEvent.NavigateToRequestPickup(listing.id)
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Icon(Icons.Default.ShoppingCart, null, Modifier.size(18.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Request Pickup")
                                                }
                                            }
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