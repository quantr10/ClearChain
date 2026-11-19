package com.clearchain.app.presentation.pendingreview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.clearchain.app.R
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.BrandHeader
import com.clearchain.app.presentation.components.BrandScaffold
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainButtonDefaults
import com.clearchain.app.presentation.components.ClearChainOutlinedButton
import com.clearchain.app.presentation.components.SectionCard
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.CardPadding
import com.clearchain.app.ui.theme.ControlIconSize
import com.clearchain.app.ui.theme.DetailIconSize
import com.clearchain.app.ui.theme.IconGap
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.ShapeMedium
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

    BrandScaffold(
        header = BrandHeader.Compact(
            title = stringResource(
                if (rejected) {
                    R.string.pending_review_rejected_title
                } else {
                    R.string.pending_review_pending_title
                }
            )
        ),
        snackbarHostState = snackbarHostState
    ) {
        Text(
            text = stringResource(
                if (rejected) {
                    R.string.pending_review_rejected_subtitle
                } else {
                    R.string.pending_review_pending_subtitle
                },
                user?.name.orEmpty()
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ── Rejection reason ─────────────────────────────────────────────────
        if (rejected && !user.verificationNotes.isNullOrBlank()) {
            Surface(
                shape = ShapeMedium,
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(CardPadding),
                    verticalArrangement = Arrangement.spacedBy(IconGap)
                ) {
                    Text(
                        stringResource(R.string.pending_review_reason_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        user.verificationNotes,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // ── What happens next ────────────────────────────────────────────────
        if (!rejected) {
            SectionCard(title = "") {
                StepRow(Icons.Default.CheckCircle, BrandGreen, stringResource(R.string.pending_review_step_submitted))
                StepRow(
                    Icons.Default.Schedule,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.pending_review_step_reviewing)
                )
                StepRow(
                    Icons.Default.Notifications,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    stringResource(R.string.pending_review_step_notify)
                )
            }
        }

        // ── Submitted details ────────────────────────────────────────────────
        if (user != null) {
            SectionCard(title = stringResource(R.string.pending_review_submitted_label)) {
                DetailRow(Icons.Default.Business, user.name)
                val (typeLabel, typeIcon) = organizationTypeLabel(user.type)
                DetailRow(typeIcon, typeLabel)
                DetailRow(Icons.Default.Email, user.email)
                if (user.phone.isNotBlank()) DetailRow(Icons.Default.Phone, user.phone)
                if (user.location.isNotBlank()) DetailRow(Icons.Default.Place, user.location)
                DetailRow(
                    Icons.Default.Description,
                    stringResource(
                        if (user.documentUrl != null) {
                            R.string.pending_review_doc_attached
                        } else {
                            R.string.pending_review_doc_missing
                        }
                    )
                )
            }
        }

        // ── Actions ──────────────────────────────────────────────────────────
        if (rejected) {
            ClearChainButton(
                text = stringResource(R.string.pending_review_fix_and_resubmit),
                onClick = onEditProfile,
                enabled = !state.isBusy,
                icon = Icons.Default.Edit,
                modifier = Modifier.fillMaxWidth()
            )
            ClearChainOutlinedButton(
                text = stringResource(R.string.pending_review_check_status),
                onClick = { viewModel.refresh() },
                loading = state.isRefreshing,
                enabled = !state.isBusy,
                icon = Icons.Default.Refresh,
                fillMaxWidth = true
            )
        } else {
            ClearChainButton(
                text = stringResource(R.string.pending_review_check_status),
                onClick = { viewModel.refresh() },
                loading = state.isRefreshing,
                enabled = !state.isBusy,
                icon = Icons.Default.Refresh,
                modifier = Modifier.fillMaxWidth()
            )
            ClearChainOutlinedButton(
                text = stringResource(R.string.pending_review_edit_profile),
                onClick = onEditProfile,
                enabled = !state.isBusy,
                icon = Icons.Default.Edit,
                fillMaxWidth = true
            )
        }

        HelpAndLogOut(
            onOpenHelp = onOpenHelp,
            onLogout = { viewModel.logout() },
            enabled = !state.isBusy,
            loggingOut = state.isLoggingOut
        )
    }
}

/**
 * Help and Log out: side by side when each label fits on one line in half the width, otherwise
 * stacked at full width, as happens with large text or long Vietnamese labels.
 */
@Composable
private fun HelpAndLogOut(
    onOpenHelp: () -> Unit,
    onLogout: () -> Unit,
    enabled: Boolean,
    loggingOut: Boolean
) {
    val helpLabel = stringResource(R.string.help)
    val logoutLabel = stringResource(R.string.pending_review_logout)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val density = LocalDensity.current

    val help = @Composable { modifier: Modifier ->
        ClearChainOutlinedButton(
            text = helpLabel,
            onClick = onOpenHelp,
            enabled = enabled,
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            fillMaxWidth = true,
            modifier = modifier
        )
    }
    val logout = @Composable { modifier: Modifier ->
        ClearChainOutlinedButton(
            text = logoutLabel,
            onClick = onLogout,
            enabled = enabled,
            loading = loggingOut,
            icon = Icons.AutoMirrored.Filled.Logout,
            contentColor = MaterialTheme.colorScheme.error,
            fillMaxWidth = true,
            modifier = modifier
        )
    }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val half = (maxWidth - ListGap) / 2
        val chrome = ControlIconSize + IconGap + ClearChainButtonDefaults.HorizontalPadding * 2
        val fits = listOf(helpLabel, logoutLabel).all { label ->
            with(density) { measurer.measure(label, labelStyle).size.width.toDp() } + chrome <= half
        }
        if (fits) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                help(Modifier.weight(1f))
                logout(Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                help(Modifier)
                logout(Modifier)
            }
        }
    }
}

/** The type row's wording and icon: the ones the organization picked on Register. */
@Composable
private fun organizationTypeLabel(type: OrganizationType): Pair<String, ImageVector> = when (type) {
    OrganizationType.GROCERY -> stringResource(R.string.role_grocery_title) to Icons.Default.Store
    OrganizationType.NGO -> stringResource(R.string.role_ngo_title) to Icons.Default.VolunteerActivism
    OrganizationType.ADMIN -> stringResource(R.string.role_admin) to Icons.Default.AdminPanelSettings
}

@Composable
private fun StepRow(icon: ImageVector, tint: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IconGap)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(ControlIconSize))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DetailRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(IconGap)
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(DetailIconSize)
        )
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
