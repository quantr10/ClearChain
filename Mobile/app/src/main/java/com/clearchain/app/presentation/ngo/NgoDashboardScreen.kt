package com.clearchain.app.presentation.ngo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NgoDashboardScreen(
    navController: NavController,
    viewModel: NgoDashboardViewModel = hiltViewModel()
) {
    val userName by viewModel.userName.collectAsState()
    val stats by viewModel.stats.collectAsState()

    Scaffold(
        topBar = {
            ClearChainTopBar(
                userName = userName,
                userType = "NGO",
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Welcome ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Welcome, $userName!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Help reduce food waste in your community",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Quick Stats (2×2) ───────────────────────────────────────
            SectionHeader(title = "Overview")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.Inventory,
                    label = "In Stock",
                    value = stats?.inStock?.toString() ?: "–",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Pending,
                    label = "Active Requests",
                    value = stats?.activeRequests?.toString() ?: "–",
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.VolunteerActivism,
                    label = "Distributed",
                    value = stats?.distributed?.toString() ?: "–",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.RestaurantMenu,
                    label = "Available Food",
                    value = stats?.availableFood?.toString() ?: "–",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Quick Actions ───────────────────────────────────────────
            SectionHeader(title = "Quick Actions")

            DashboardActionCard(
                icon = Icons.Default.RestaurantMenu,
                title = "Browse Food",
                subtitle = "Find available surplus food",
                onClick = { navController.navigate(Screen.BrowseListings.route) }
            )

            DashboardActionCard(
                icon = Icons.Default.LocalShipping,
                title = "My Requests",
                subtitle = "View and track your pickup requests",
                onClick = { navController.navigate(Screen.MyRequests.route) }
            )

            DashboardActionCard(
                icon = Icons.Default.Inventory,
                title = "Inventory",
                subtitle = "Manage collected food items",
                onClick = { navController.navigate(Screen.Inventory.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
