package com.clearchain.app.presentation.ngo.browselistings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    // First time with no location → redirect to location picker
    LaunchedEffect(state.isLocationSet, state.isCheckingLocation) {
        if (!state.isCheckingLocation && !state.isLocationSet) {
            navController.navigate("location_picker") {
                launchSingleTop = true
            }
        }
    }

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
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.allListings.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.error != null && state.allListings.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Something went wrong",
                        subtitle = state.error,
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(BrowseListingsEvent.LoadListings) }
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // ── Location Bar ─────────────────────────
                        if (state.isLocationSet) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { navController.navigate("location_picker_edit") },
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, null, Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            state.locationDisplayName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "Within ${state.radiusKm} km",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(Icons.Default.Settings, "Change location",
                                        Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                                }
                            }
                        }

                        // ── Search ───────────────────────────────
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChange = { viewModel.onEvent(BrowseListingsEvent.SearchQueryChanged(it)) },
                            placeholder = "Search by name, grocery, location...",
                            modifier = Modifier.padding(16.dp)
                        )

                        // ── Category chips ───────────────────────
                        FilterChipsRow(
                            filters = state.availableCategoryFilters,
                            selectedFilter = state.selectedCategory,
                            onFilterSelected = { viewModel.onEvent(BrowseListingsEvent.CategoryFilterChanged(it)) },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // ── Results count + Sort ─────────────────
                        ResultsCountAndSort(
                            count = state.filteredListings.size,
                            itemName = "listing",
                            selectedSort = state.selectedSort,
                            onSortSelected = { viewModel.onEvent(BrowseListingsEvent.SortOptionChanged(it)) },
                            sortOptions = state.availableSortOptions,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(BrowseListingsEvent.ClearError) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }

                        when {
                            state.filteredListings.isEmpty() -> {
                                EmptyState(
                                    icon = if (state.allListings.isEmpty()) Icons.Default.SearchOff
                                    else Icons.Default.FilterAlt,
                                    title = if (state.allListings.isEmpty()) "No available listings nearby"
                                    else "No listings match your filters",
                                    subtitle = if (state.allListings.isEmpty())
                                        "Try increasing your distance or check back later"
                                    else "Try adjusting your search or filters"
                                )
                            }

                            else -> {
                                PullToRefreshBox(
                                    isRefreshing = state.isRefreshing,
                                    onRefresh = { viewModel.onEvent(BrowseListingsEvent.RefreshListings) }
                                ) {
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items = state.filteredListings, key = { it.id }) { listing ->
                                            ListingCard(
                                                listing = listing,
                                                showGroceryInfo = true,
                                                modifier = Modifier.clickable {
                                                    navController.navigate("listing_detail/${listing.id}")
                                                },
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
}