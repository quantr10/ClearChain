// ═══════════════════════════════════════════════════════════════════════════════
// GroceryDashboardScreen.kt — REDESIGNED with stats + unified components
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.grocery

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
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryDashboardScreen(
    navController: NavController,
    viewModel: GroceryDashboardViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    var userName by remember { mutableStateOf("Grocery Store") }

    LaunchedEffect(key1 = true) {
        scope.launch {
            viewModel.getCurrentUserUseCase().first()?.let { user ->
                userName = user.name
            }
        }
    }

    Scaffold(
        topBar = {
            ClearChainTopBar(
                userName = userName,
                userType = "Grocery",
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
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when {
                    hour < 12 -> "morning"
                    hour < 17 -> "afternoon"
                    else -> "evening"
                }
                Text(
                    text = "Good $greeting, $userName!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your surplus food listings",
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
                    label = "Active Listings",
                    value = "–",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.LocalShipping,
                    label = "Pending Requests",
                    value = "–",
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
                    value = "–",
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    icon = Icons.Default.Eco,
                    label = "Food Saved",
                    value = "–",
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Quick Actions ───────────────────────────────────────────
            SectionHeader(title = "Quick Actions")

            DashboardActionCard(
                icon = Icons.Default.AddCircle,
                title = "Create Listing",
                subtitle = "Add new surplus food items",
                onClick = { navController.navigate(Screen.CreateListing.route) }
            )

            DashboardActionCard(
                icon = Icons.Default.List,
                title = "My Listings",
                subtitle = "View and manage your listings",
                onClick = { navController.navigate(Screen.MyListings.route) }
            )

            DashboardActionCard(
                icon = Icons.Default.LocalShipping,
                title = "Pickup Requests",
                subtitle = "Manage pickup requests from NGOs",
                onClick = { navController.navigate(Screen.PickupRequests.route) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}