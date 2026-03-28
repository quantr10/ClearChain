package com.clearchain.app.presentation.admin.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequestDetail: (String) -> Unit = {},  // NEW (Part 3)
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFullPhotoUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(TransactionsEvent.SearchQueryChanged(it)) },
                placeholder = "Search by item, grocery, NGO...",
                modifier = Modifier.padding(16.dp)
            )

            val statuses = listOf(
                FilterChipData(null, "All"),
                FilterChipData("PENDING", "Pending"),
                FilterChipData("APPROVED", "Approved"),
                FilterChipData("READY", "Ready"),
                FilterChipData("COMPLETED", "Completed"),
            )

            FilterChipsRow(
                filters = statuses,
                selectedFilter = state.selectedStatus,
                onFilterSelected = { viewModel.onEvent(TransactionsEvent.StatusFilterChanged(it)) },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "${state.filteredTransactions.size} transaction${if (state.filteredTransactions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                state.isLoading && state.allTransactions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.allTransactions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Receipt,
                        title = "No transactions yet",
                        subtitle = "Transactions will appear here when requests are created"
                    )
                }

                state.filteredTransactions.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.FilterAlt,
                        title = "No transactions match your filters",
                        subtitle = "Try adjusting your search or filters"
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.onEvent(TransactionsEvent.RefreshTransactions) }
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.filteredTransactions, key = { it.id }) { transaction ->
                                // ═══ UPDATED (Part 3): Tap card → RequestDetailScreen ═══
                                Box(
                                    modifier = Modifier.clickable {
                                        onNavigateToRequestDetail(transaction.id)
                                    }
                                ) {
                                    RequestCard(
                                        request = transaction,
                                        viewMode = RequestViewMode.ADMIN,
                                        onViewPhoto = { showFullPhotoUrl = it }
                                    )
                                }
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}