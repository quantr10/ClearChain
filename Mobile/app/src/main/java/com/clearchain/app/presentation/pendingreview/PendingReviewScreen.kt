package com.clearchain.app.presentation.pendingreview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.clearchain.app.R
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainOutlinedButton
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent

@Composable
fun PendingReviewScreen(
    onNavigateRoute: (String) -> Unit,
    onEditProfile: () -> Unit,
    onOpenHelp: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: PendingReviewViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // This is a gate — there is nowhere to go "back" to.
    BackHandler(enabled = true) {}

    // Re-check verification status whenever the screen comes back to the foreground
    // (e.g. after tapping an "approved" push notification).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh(silent = true) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                is UiEvent.Navigate -> if (event.route == "login") onLoggedOut() else onNavigateRoute(event.route)
                else -> {}
            }
        }
    }

    val user = state.user
    val rejected = user?.verificationStatus == VerificationStatus.REJECTED

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Status hero ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val (icon, tint) = if (rejected)
                    Icons.Default.ErrorOutline to MaterialTheme.colorScheme.error
                else
                    Icons.Default.HourglassTop to MaterialTheme.colorScheme.secondary

                Surface(
                    shape = RoundedCornerShape(50),
                    color = tint.copy(alpha = 0.12f),
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(44.dp))
                    }
                }

                Text(
                    text = stringResource(
                        if (rejected) R.string.pending_review_rejected_title
                        else R.string.pending_review_pending_title
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(
                        if (rejected) R.string.pending_review_rejected_subtitle
                        else R.string.pending_review_pending_subtitle
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ── Rejection reason ─────────────────────────────────────────────
            if (rejected && !user?.verificationNotes.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            stringResource(R.string.pending_review_reason_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            user!!.verificationNotes!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── What happens next ────────────────────────────────────────────
            if (!rejected) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StepRow(Icons.Default.CheckCircle, BrandGreen, stringResource(R.string.pending_review_step_submitted))
                        StepRow(Icons.Default.Schedule, MaterialTheme.colorScheme.secondary, stringResource(R.string.pending_review_step_reviewing))
                        StepRow(Icons.Default.Notifications, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.pending_review_step_notify))
                    }
                }
            }

            // ── Submitted details ────────────────────────────────────────────
            if (user != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.pending_review_submitted_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        DetailRow(Icons.Default.Business, user.name)
                        DetailRow(Icons.Default.Category, user.type.name.lowercase().replaceFirstChar { it.uppercase() })
                        DetailRow(Icons.Default.Email, user.email)
                        if (user.phone.isNotBlank()) DetailRow(Icons.Default.Phone, user.phone)
                        if (user.location.isNotBlank()) DetailRow(Icons.Default.Place, user.location)
                        DetailRow(
                            Icons.Default.Description,
                            stringResource(
                                if (user.documentUrl != null) R.string.pending_review_doc_attached
                                else R.string.pending_review_doc_missing
                            )
                        )
                    }
                }
            }

            // ── Actions ──────────────────────────────────────────────────────
            ClearChainButton(
                text = stringResource(R.string.pending_review_check_status),
                onClick = { viewModel.refresh() },
                loading = state.isRefreshing,
                enabled = !state.isRefreshing,
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth()
            )
            ClearChainOutlinedButton(
                text = stringResource(
                    if (rejected) R.string.pending_review_fix_and_resubmit
                    else R.string.pending_review_edit_profile
                ),
                onClick = onEditProfile,
                icon = Icons.Default.Edit,
                fillMaxWidth = true
            )

            ClearChainOutlinedButton(
                text = stringResource(R.string.help),
                onClick = onOpenHelp,
                icon = Icons.Default.HelpOutline,
                fillMaxWidth = true
            )

            ClearChainOutlinedButton(
                text = stringResource(R.string.pending_review_logout),
                onClick = { viewModel.logout() },
                icon = Icons.Default.Logout,
                contentColor = MaterialTheme.colorScheme.error,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                fillMaxWidth = true
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepRow(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
