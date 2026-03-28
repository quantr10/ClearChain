// ═══════════════════════════════════════════════════════════════════════════════
// VerificationQueueScreen.kt — REDESIGNED with unified components
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.admin.verification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationQueueScreen(
    onNavigateBack: () -> Unit,
    viewModel: VerificationQueueViewModel = hiltViewModel()
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
                title = { Text("Organizations") },
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
                state.isLoading && state.organizations.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.organizations.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Business,
                        title = "No organizations yet",
                        subtitle = "Organizations will appear here when they register"
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.onEvent(VerificationQueueEvent.RefreshOrganizations) }
                    ) {                     
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "${state.organizations.size} organization${if (state.organizations.size != 1) "s" else ""} registered",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items(state.organizations, key = { it.id }) { org ->
                                OrganizationCard(organization = org)
                            }

                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganizationCard(organization: Organization) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = organization.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // Type badge
                    StatusBadge(
                        label = organization.type.name,
                        backgroundColor = when (organization.type) {
                            OrganizationType.GROCERY -> MaterialTheme.colorScheme.secondaryContainer
                            OrganizationType.NGO -> MaterialTheme.colorScheme.tertiaryContainer
                            OrganizationType.ADMIN -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = when (organization.type) {
                            OrganizationType.GROCERY -> MaterialTheme.colorScheme.onSecondaryContainer
                            OrganizationType.NGO -> MaterialTheme.colorScheme.onTertiaryContainer
                            OrganizationType.ADMIN -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }

                // Verified badge
                StatusBadge(
                    label = "Verified",
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.CheckCircle
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Details ─────────────────────────────────────────────
            InfoRow(Icons.Default.Email, "Email", organization.email)
            InfoRow(Icons.Default.Phone, "Phone", organization.phone.ifBlank { "Not provided" })
            InfoRow(Icons.Default.Place, "Location", organization.location.ifBlank { "Not provided" })
            if (organization.address.isNotBlank()) {
                InfoRow(Icons.Default.Home, "Address", organization.address)
            }
        }
    }
}