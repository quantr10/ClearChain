package com.clearchain.app.presentation.admin.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.presentation.admin.analytics.AnalyticsSection
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.ui.theme.StatusColors
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val adminDefault = stringResource(R.string.label_admin)
    var userName by remember { mutableStateOf(adminDefault) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }

    // Collected rather than read once, so a freshly uploaded avatar shows up here
    // without a re-login.
    LaunchedEffect(true) {
        scope.launch {
            viewModel.getCurrentUserUseCase().collect { user ->
                if (user != null) {
                    userName = user.name
                    profilePictureUrl = user.profilePictureUrl
                }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        HapticPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { viewModel.onEvent(AdminDashboardEvent.RefreshStats) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Gradient admin header ────────────────────────────────────────
                DashboardWelcomeHeader(
                    userName = userName,
                    subtitle = stringResource(R.string.system_health),
                    roleLabel = stringResource(R.string.role_admin),
                    profilePictureUrl = profilePictureUrl,
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.primary,
                        BrandTeal
                    ),
                    onProfileClick = { navController.navigate(Screen.AccountDetail.route) },
                    onNotificationsClick = { navController.navigate(Screen.NotificationInbox.route) }
                )

                Column(
                    modifier = Modifier.padding(ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.isLoading && state.stats == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // ── Platform stats ───────────────────────────────────────
                        state.stats?.let { stats ->
                            // Listing supply only. Request counts live in the status breakdown
                            // and the completion meter below, so they are not repeated here.
                            // These are live counts, so the tile opens the analytics backlog card.
                            DrillDownSection(
                                title = stringResource(R.string.section_listings),
                                section = AnalyticsSection.BACKLOG,
                                navController = navController
                            ) {
                                ListingOverview(
                                    activeCount = "${stats.activeListings}",
                                    reservedCount = "${stats.reservedListings}",
                                    expiredCount = "${stats.expiredListings}"
                                )
                            }

                            // Completion rate meter
                            if (stats.totalPickupRequests > 0) {
                                val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                                DrillDownSection(
                                    // The meter inside already names itself, next to the figure
                                    // it belongs to; a section title above would say it twice.
                                    title = "",
                                    section = AnalyticsSection.REQUESTS,
                                    navController = navController
                                ) {
                                    // Same layout as the NGO dashboard's weekly goal card.
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = stringResource(R.string.stat_completion_rate),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = "$rate%",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { rate / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            color = if (rate >= 100) BrandGreen else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = stringResource(R.string.admin_completion_detail, stats.completedRequests, stats.totalPickupRequests),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // ── Request Status Bar Chart ─────────────────────────────
                        state.stats?.let { stats ->
                            val barData = listOf(
                                BarData(stringResource(R.string.status_pending), stats.pendingRequests, StatusColors.Pending),
                                BarData(stringResource(R.string.status_approved), stats.approvedRequests, StatusColors.Approved),
                                BarData(stringResource(R.string.status_ready), stats.readyRequests, StatusColors.Ready),
                                BarData(stringResource(R.string.status_completed), stats.completedRequests, StatusColors.Completed),
                                BarData(stringResource(R.string.status_cancelled), stats.cancelledRequests, StatusColors.Expired)
                            )
                            if (barData.any { it.value > 0 }) {
                                DrillDownSection(
                                    title = stringResource(R.string.section_request_status_breakdown),
                                    section = AnalyticsSection.REQUESTS,
                                    navController = navController
                                ) {
                                    ColumnBarChart(bars = barData)
                                }
                            }

                            // ── Org Type Donut Chart ─────────────────────────────
                            if (stats.totalOrganizations > 0) {
                                DrillDownSection(
                                    title = stringResource(R.string.section_organizations),
                                    section = AnalyticsSection.ORGANIZATIONS,
                                    navController = navController
                                ) {
                                    // One dimension only: an unverified NGO is still an NGO, so
                                    // verification is not a third slice here.
                                    DonutChart(
                                        slices = listOf(
                                            BarData(stringResource(R.string.org_type_groceries), stats.totalGroceries, StatusColors.Available),
                                            BarData(stringResource(R.string.org_type_ngos), stats.totalNgos, StatusColors.Approved)
                                        ),
                                        centerValue = stats.totalOrganizations.toString(),
                                        centerLabel = stringResource(R.string.stat_total),
                                        legendFirst = true
                                    )
                                }
                            }
                        }
                    }

                    // ── Quick actions ────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardActionCard(
                            icon = Icons.Default.VerifiedUser,
                            title = stringResource(R.string.verification_queue),
                            subtitle = stringResource(R.string.admin_review_organizations),
                            onClick = { navController.navigate(Screen.Verification.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.History,
                            title = stringResource(R.string.transactions),
                            subtitle = stringResource(R.string.admin_view_transactions),
                            onClick = { navController.navigate(Screen.Transactions.route) }
                        )
                        DashboardActionCard(
                            icon = Icons.Default.BarChart,
                            title = stringResource(R.string.statistics),
                            subtitle = stringResource(R.string.system_health),
                            onClick = { navController.navigate(Screen.AdminStatistics.route) }
                        )
                    }

                    // ── Recent activity feed ─────────────────────────────────────
                    if (state.recentActivities.isNotEmpty()) {
                        DashboardSection(title = stringResource(R.string.section_recent_activity)) {
                            // Straight onto the section — the tinted Surface that used to wrap
                            // this was a second rounded box inside the section's own.
                            Column(modifier = Modifier.fillMaxWidth()) {
                                state.recentActivities.take(5).forEachIndexed { index, activity ->
                                    ActivityRow(activity = activity)
                                    if (index < minOf(4, state.recentActivities.size - 1)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Alert feed ───────────────────────────────────────────────
                    if (state.alertFeedItems.isNotEmpty()) {
                        DashboardSection(title = stringResource(R.string.section_alert_feed)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.alertFeedItems.take(5).forEach { alert ->
                                    val (bgColor, iconColor, icon) = when (alert.severity.lowercase()) {
                                        "high" -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, Icons.Default.Warning)
                                        "medium" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Info)
                                        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Notifications)
                                    }
                                    Surface(color = bgColor, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp).padding(top = 2.dp))
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(alert.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                                Text(alert.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("${alert.type.replaceFirstChar { it.uppercase() }} • ${alert.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Drill-down sections ──────────────────────────────────────────────────────

/**
 * A dashboard card whose figures are a summary of one analytics section, and which opens
 * that section when tapped. The home screen shows the headline; the number keeps its
 * meaning on the next screen because it lands on the card that explains it rather than at
 * the top of a long report.
 */
@Composable
private fun DrillDownSection(
    title: String,
    section: AnalyticsSection,
    navController: NavController,
    content: @Composable ColumnScope.() -> Unit
) {
    DashboardSection(
        title = title,
        modifier = Modifier.clickable {
            navController.navigate(Screen.AdminStatistics.createRoute(section.key))
        },
        content = content
    )
}

// ── Request Status Bar Chart (Canvas) ────────────────────────────────────────

// ── Overview tiles ───────────────────────────────────────────────────────────

/**
 * Listing supply at a glance: what is still open is the number an admin acts on,
 * so it carries the row, with the two follow-up states stacked beside it.
 */
@Composable
private fun ListingOverview(
    activeCount: String,
    reservedCount: String,
    expiredCount: String,
    modifier: Modifier = Modifier
) {
    Row(
        // Intrinsic height ties the hero tile to the two stacked beside it.
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = activeCount,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.status_active),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryListingTile(
                icon = Icons.Default.Bookmark,
                label = stringResource(R.string.stat_reserved),
                value = reservedCount,
                modifier = Modifier.weight(1f)
            )
            SecondaryListingTile(
                icon = Icons.Default.EventBusy,
                label = stringResource(R.string.status_expired),
                value = expiredCount,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SecondaryListingTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ActivityRow(activity: AdminActivity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(activity.icon, style = MaterialTheme.typography.labelMedium)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = activity.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = activity.timestamp.take(10),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
