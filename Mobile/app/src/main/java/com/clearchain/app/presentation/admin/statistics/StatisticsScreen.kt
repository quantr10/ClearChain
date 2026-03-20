// ═══════════════════════════════════════════════════════════════════════════════
// StatisticsScreen.kt — REDESIGNED with shared components
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.admin.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.AdminStats
import com.clearchain.app.presentation.components.SectionHeader
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
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
                title = { Text("Detailed Statistics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(StatisticsEvent.RefreshStatistics) }
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
            if (state.isLoading && state.stats == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                state.stats?.let { stats ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Organizations ────────────────────────────────
                        SectionHeader("Organizations")
                        StatsGroup(
                            icon = Icons.Default.Business,
                            title = "Organization Overview",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            items = listOf(
                                "Total" to "${stats.totalOrganizations}",
                                "Groceries" to "${stats.totalGroceries}",
                                "NGOs" to "${stats.totalNgos}",
                                "Verified" to "${stats.verifiedOrganizations}",
                                "Unverified" to "${stats.unverifiedOrganizations}"
                            )
                        )

                        // ── Listings ────────────────────────────────────
                        SectionHeader("Listings")
                        StatsGroup(
                            icon = Icons.Default.Inventory,
                            title = "Food Listings",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            items = listOf(
                                "Total" to "${stats.totalListings}",
                                "Active (Open)" to "${stats.activeListings}",
                                "Reserved" to "${stats.reservedListings}"
                            )
                        )

                        // ── Pickup Requests ─────────────────────────────
                        SectionHeader("Pickup Requests")
                        StatsGroup(
                            icon = Icons.Default.LocalShipping,
                            title = "Request Status Breakdown",
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            items = listOf(
                                "Total" to "${stats.totalPickupRequests}",
                                "Pending" to "${stats.pendingRequests}",
                                "Approved" to "${stats.approvedRequests}",
                                "Ready" to "${stats.readyRequests}",
                                "Completed" to "${stats.completedRequests}",
                                "Cancelled" to "${stats.cancelledRequests}"
                            )
                        )

                        // ── Platform Impact ─────────────────────────────
                        SectionHeader("Platform Impact")

                        // Food Saved Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Eco, null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        "Total Food Saved",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${stats.totalFoodSaved.toInt()} items",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Completion Rate Card
                        if (stats.totalPickupRequests > 0) {
                            val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.TrendingUp, null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(
                                            "Completion Rate",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        "$rate%",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    LinearProgressIndicator(
                                        progress = { rate / 100f },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ── Stats Group Card ────────────────────────────────────────────────────────

@Composable
private fun StatsGroup(
    icon: ImageVector,
    title: String,
    containerColor: Color,
    contentColor: Color,
    items: List<Pair<String, String>>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, null, Modifier.size(28.dp), tint = contentColor)
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            HorizontalDivider(color = contentColor.copy(alpha = 0.2f))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f))
                    Text(value, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = contentColor)
                }
            }
        }
    }
}