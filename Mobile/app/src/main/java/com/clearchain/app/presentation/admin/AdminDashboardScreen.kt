// ═══════════════════════════════════════════════════════════════════════════════
// AdminDashboardScreen.kt — REDESIGNED with unified components
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("Admin") }

    LaunchedEffect(key1 = true) {
        scope.launch {
            viewModel.getCurrentUserUseCase().first()?.let { user ->
                userName = user.name
            }
        }
    }

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
            ClearChainTopBar(
                userName = userName,
                userType = "Admin",
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(AdminDashboardEvent.RefreshStats) }
                    ) {
                        if (state.isRefreshing) {
                            CircularProgressIndicator(
                                Modifier.size(24.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // ── Welcome ──────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Monitor platform activity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ── Error ────────────────────────────────────────────
                    state.error?.let {
                        ErrorBanner(
                            message = it,
                            onDismiss = { viewModel.onEvent(AdminDashboardEvent.ClearError) }
                        )
                    }

                    // ── Platform Stats (2×2) ────────────────────────────
                    state.stats?.let { stats ->
                        SectionHeader(title = "Platform Overview")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                icon = Icons.Default.Business,
                                label = "Organizations",
                                value = "${stats.totalOrganizations}",
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.Inventory,
                                label = "Active Listings",
                                value = "${stats.activeListings}",
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                icon = Icons.Default.CheckCircle,
                                label = "Completed",
                                value = "${stats.completedRequests}",
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                icon = Icons.Default.Eco,
                                label = "Food Saved",
                                value = "${stats.totalFoodSaved.toInt()}",
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // ── Completion Rate ─────────────────────────────
                        if (stats.totalPickupRequests > 0) {
                            val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
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
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        "$rate%",
                                        style = MaterialTheme.typography.headlineMedium,
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
                    }

                    // ── Quick Actions ────────────────────────────────────
                    SectionHeader(title = "Quick Actions")

                    DashboardActionCard(
                        icon = Icons.Default.VerifiedUser,
                        title = "Organizations",
                        subtitle = "${state.stats?.totalOrganizations ?: 0} registered",
                        onClick = { navController.navigate("admin/verification") }
                    )

                    DashboardActionCard(
                        icon = Icons.Default.Analytics,
                        title = "Detailed Statistics",
                        subtitle = "View comprehensive analytics",
                        onClick = { navController.navigate("admin/statistics") }
                    )

                    DashboardActionCard(
                        icon = Icons.Default.History,
                        title = "Transaction History",
                        subtitle = "${state.stats?.totalPickupRequests ?: 0} total requests",
                        onClick = { navController.navigate("admin/transactions") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}