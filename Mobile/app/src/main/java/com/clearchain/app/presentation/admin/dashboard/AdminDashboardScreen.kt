package com.clearchain.app.presentation.admin.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    navController: NavController,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val state            by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    val adminDefault = stringResource(R.string.label_admin)
    var userName by remember { mutableStateOf(adminDefault) }

    LaunchedEffect(true) {
        scope.launch {
            viewModel.getCurrentUserUseCase().first()?.let { user -> userName = user.name }
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
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Gradient admin header ──────────────────────────────────────
            DashboardWelcomeHeader(
                userName       = userName,
                subtitle       = stringResource(R.string.system_health),
                roleLabel      = stringResource(R.string.role_admin),
                gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    BrandTeal
                ),
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                trailingContent = {
                    Spacer(Modifier.height(4.dp))
                    // Refresh indicator
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier   = Modifier.size(20.dp),
                            color      = Color.White.copy(alpha = 0.7f),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.onEvent(AdminDashboardEvent.RefreshStats) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh),
                                tint     = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )

            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── Error banner ───────────────────────────────────────────
                AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut()) {
                    AlertBanner(
                        message = state.error ?: "",
                        type    = AlertType.ERROR,
                        icon    = Icons.Default.ErrorOutline
                    )
                }

                if (state.isLoading && state.stats == null) {
                    Box(
                        modifier         = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // ── Platform stats ─────────────────────────────────────
                    state.stats?.let { stats ->
                        DashboardSection(title = stringResource(R.string.section_overview)) {
                            StatCardGrid(
                                stats = listOf(
                                    Triple(Icons.Default.Business,      stringResource(R.string.verification_queue), "${stats.totalOrganizations}"),
                                    Triple(Icons.Default.Inventory,     stringResource(R.string.stat_active_listings), "${stats.activeListings}"),
                                    Triple(Icons.Default.CheckCircle,   stringResource(R.string.stat_completed),       "${stats.completedRequests}"),
                                    Triple(Icons.Default.Eco,           stringResource(R.string.stat_food_saved_kg),   "${stats.totalFoodSaved.toInt()}")
                                ),
                                accentColors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary,
                                    BrandGreen
                                )
                            )
                        }

                        // Completion rate meter
                        if (stats.totalPickupRequests > 0) {
                            val rate = (stats.completedRequests.toFloat() / stats.totalPickupRequests * 100).toInt()
                            Surface(
                                color    = MaterialTheme.colorScheme.primaryContainer,
                                shape    = MaterialTheme.shapes.large,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier            = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.TrendingUp,
                                            null,
                                            tint     = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            stringResource(R.string.stat_completion_rate),
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color      = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            "$rate%",
                                            style      = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress      = { rate / 100f },
                                        modifier      = Modifier.fillMaxWidth(),
                                        color         = MaterialTheme.colorScheme.primary,
                                        trackColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        strokeCap     = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    Text(
                                        stringResource(R.string.admin_completion_detail, stats.completedRequests, stats.totalPickupRequests),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Request Status Bar Chart ───────────────────────────
                    state.stats?.let { stats ->
                        val barData = listOf(
                            stringResource(R.string.status_pending)   to stats.pendingRequests,
                            stringResource(R.string.status_approved)  to stats.approvedRequests,
                            stringResource(R.string.status_ready)     to stats.readyRequests,
                            stringResource(R.string.status_completed) to stats.completedRequests,
                            stringResource(R.string.status_cancelled) to stats.cancelledRequests
                        )
                        if (barData.any { it.second > 0 }) {
                            DashboardSection(title = stringResource(R.string.section_request_status_breakdown)) {
                                RequestStatusBarChart(data = barData)
                            }
                        }

                        // ── Org Type Donut Chart ───────────────────────────
                        if (stats.totalOrganizations > 0) {
                            DashboardSection(title = stringResource(R.string.section_org_types)) {
                                OrgTypeDonutChart(
                                    groceries  = stats.totalGroceries,
                                    ngos       = stats.totalNgos,
                                    unverified = stats.unverifiedOrganizations
                                )
                            }
                        }
                    }
                }

                // ── Quick actions ──────────────────────────────────────────
                DashboardSection(title = stringResource(R.string.admin_dashboard)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashboardActionCard(
                            icon     = Icons.Default.VerifiedUser,
                            title    = stringResource(R.string.verification_queue),
                            subtitle = stringResource(R.string.admin_pending_approvals, state.stats?.unverifiedOrganizations ?: 0),
                            badge    = state.stats?.unverifiedOrganizations?.takeIf { it > 0 }?.toString(),
                            onClick  = { navController.navigate(Screen.Verification.route) }
                        )
                        DashboardActionCard(
                            icon     = Icons.Default.History,
                            title    = stringResource(R.string.transactions),
                            subtitle = stringResource(R.string.admin_total_requests, state.stats?.totalPickupRequests ?: 0),
                            onClick  = { navController.navigate(Screen.Transactions.route) }
                        )
                        DashboardActionCard(
                            icon     = Icons.Default.BarChart,
                            title    = stringResource(R.string.statistics),
                            subtitle = stringResource(R.string.system_health),
                            onClick  = { navController.navigate(Screen.AdminStatistics.route) }
                        )
                    }
                }

                // ── Recent activity feed ───────────────────────────────────
                if (state.recentActivities.isNotEmpty()) {
                    DashboardSection(title = stringResource(R.string.section_recent_activity)) {
                        Surface(
                            color    = MaterialTheme.colorScheme.surfaceVariant,
                            shape    = MaterialTheme.shapes.large,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                state.recentActivities.take(5).forEachIndexed { index, activity ->
                                    ActivityRow(activity = activity)
                                    if (index < minOf(4, state.recentActivities.size - 1)) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Alert feed ─────────────────────────────────────────────
                if (state.alertFeedItems.isNotEmpty()) {
                    DashboardSection(title = stringResource(R.string.section_alert_feed)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.alertFeedItems.take(5).forEach { alert ->
                                val (bgColor, iconColor, icon) = when (alert.severity.lowercase()) {
                                    "high"   -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, Icons.Default.Warning)
                                    "medium" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Info)
                                    else     -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Notifications)
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

                // ── User growth chart ──────────────────────────────────────
                if (state.userGrowthData.isNotEmpty()) {
                    DashboardSection(title = stringResource(R.string.section_new_orgs_30d)) {
                        UserGrowthChart(data = state.userGrowthData)
                    }
                }

                // ── System health ──────────────────────────────────────────
                state.healthData?.let { health ->
                    DashboardSection(title = stringResource(R.string.section_system_health)) {
                        SystemHealthCard(health = health)
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Request Status Bar Chart (Canvas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RequestStatusBarChart(data: List<Pair<String, Int>>) {
    val maxValue = data.maxOfOrNull { it.second } ?: 1
    val barColors = listOf(
        Color(0xFFFF9800), // Pending - orange
        Color(0xFF2196F3), // Approved - blue
        Color(0xFF9C27B0), // Ready - purple
        Color(0xFF4CAF50), // Completed - green
        Color(0xFF9E9E9E)  // Cancelled - grey
    )

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                data.forEachIndexed { index, (label, value) ->
                    val color = barColors.getOrElse(index) { barColors.last() }
                    val heightFraction = if (maxValue > 0) value.toFloat() / maxValue else 0f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height((120 * heightFraction.coerceAtLeast(0.02f)).dp)
                                .fillMaxWidth(0.6f)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Org Type Donut Chart (Canvas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OrgTypeDonutChart(groceries: Int, ngos: Int, unverified: Int) {
    val total     = groceries + ngos
    val verified  = total - unverified
    val groceriesLabel  = stringResource(R.string.org_type_groceries)
    val ngosLabel       = stringResource(R.string.org_type_ngos)
    val unverifiedLabel = stringResource(R.string.org_type_unverified)
    val segments  = listOf(
        Triple(groceriesLabel,  groceries,  Color(0xFF4CAF50)),
        Triple(ngosLabel,       ngos,        Color(0xFF2196F3)),
        Triple(unverifiedLabel, unverified,  Color(0xFFFF5722))
    ).filter { it.second > 0 }
    val segTotal  = segments.sumOf { it.second }.coerceAtLeast(1)

    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Donut
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                val strokeWidth = size.minDimension * 0.22f
                segments.forEach { (_, value, color) ->
                    val sweep = 360f * value / segTotal
                    drawArc(
                        color       = color,
                        startAngle  = startAngle,
                        sweepAngle  = sweep,
                        useCenter   = false,
                        style       = Stroke(width = strokeWidth),
                        topLeft     = Offset(strokeWidth / 2, strokeWidth / 2),
                        size        = Size(size.width - strokeWidth, size.height - strokeWidth)
                    )
                    startAngle += sweep
                }
            }
            // Legend
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                segments.forEach { (label, value, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            "$label: $value",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    stringResource(R.string.admin_org_total_verified, total, verified),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// User Growth Line Chart (Canvas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun UserGrowthChart(data: List<com.clearchain.app.data.remote.dto.UserGrowthDay>) {
    if (data.isEmpty()) return
    val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val totalRegistered = data.sumOf { it.count }

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.admin_new_orgs_count, totalRegistered), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.label_30_days), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                val w = size.width; val h = size.height
                val step = if (data.size > 1) w / (data.size - 1) else w
                val points = data.mapIndexed { i, d ->
                    Offset(i * step, h - (d.count.toFloat() / maxCount) * h * 0.85f)
                }
                // Fill
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(w, h); close()
                }
                drawPath(path, color = fillColor)
                // Line
                points.zipWithNext().forEach { (a, b) ->
                    drawLine(lineColor, a, b, strokeWidth = 3f)
                }
                // Dots
                points.forEach { drawCircle(lineColor, radius = 4f, center = it) }
            }
            // Last/first label
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(data.firstOrNull()?.date?.take(10) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(data.lastOrNull()?.date?.take(10) ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// System Health Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SystemHealthCard(health: com.clearchain.app.data.remote.dto.AdminHealthData) {
    val isHealthy = health.status.lowercase() == "healthy"
    val statusColor = if (isHealthy) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor)
                )
                Text(
                    health.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(Modifier.weight(1f))
                Text(health.timestamp.take(16).replace("T", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // DB health
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Storage, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.label_database), style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (health.database.ok) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error))
                        Text("${health.database.latencyMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Alert counts
            val alerts = health.alerts
            if (alerts.pendingVerifications > 0 || alerts.unreadNotifications > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (alerts.pendingVerifications > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                                Text(stringResource(R.string.admin_pending_verif_count, alerts.pendingVerifications), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (alerts.unreadNotifications > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Notifications, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text(stringResource(R.string.admin_unread_notif_count, alerts.unreadNotifications), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(activity: AdminActivity) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(activity.icon, style = MaterialTheme.typography.labelMedium)
        }
        Column(
            modifier            = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text       = activity.title,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text  = activity.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text  = activity.timestamp.take(10),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
