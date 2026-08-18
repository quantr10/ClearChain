package com.clearchain.app.presentation.profile

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clearchain.app.domain.model.Organization
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.domain.model.VerificationStatus
import com.clearchain.app.presentation.components.*
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccountDetail: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val verificationMessage = when (state.user?.verificationStatus) {
        VerificationStatus.PENDING -> stringResource(R.string.msg_org_under_review)
        VerificationStatus.REJECTED -> stringResource(R.string.msg_verification_rejected_profile)
        else -> null
    }
    SnackbarMessageEffect(snackbarHostState, verificationMessage)

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    event.message, duration = SnackbarDuration.Short
                )
                is UiEvent.Navigate -> onLogout()
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.user == null ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.user != null ->
                    ProfileViewContent(
                        state = state,
                        onChangePassword = { showChangePasswordDialog = true },
                        onLogout = { showLogoutDialog = true },
                        onDeleteAccount = { showDeleteAccountDialog = true },
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToHelp = onNavigateToHelp,
                        onNavigateToAccountDetail = onNavigateToAccountDetail
                    )

                else ->
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = stringResource(R.string.error_generic),
                        subtitle = state.error,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.onEvent(ProfileEvent.Refresh) }
                    )
            }
        }
    }

    if (showLogoutDialog) {
        DestructiveConfirmDialog(
            title = stringResource(R.string.logout),
            message = "",
            confirmLabel = stringResource(R.string.logout),
            onConfirm = { showLogoutDialog = false; onLogout() },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, newPassword ->
                viewModel.onEvent(ProfileEvent.ChangePassword(current, newPassword))
                showChangePasswordDialog = false
            }
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            isLoading = state.isDeletingAccount,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { password ->
                viewModel.onEvent(ProfileEvent.DeleteAccount(password))
                showDeleteAccountDialog = false
            }
        )
    }
}

@Composable
private fun ProfileViewContent(
    state: ProfileState,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccountDetail: () -> Unit = {}
) {
    val user = state.user!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHero(user = user)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardActionCard(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.account_detail),
                    onClick = onNavigateToAccountDetail
                )
                DashboardActionCard(
                    icon = Icons.Default.BarChart,
                    title = stringResource(R.string.label_analytics),
                    onClick = onNavigateToAnalytics
                )
                DashboardActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = stringResource(R.string.label_help_faq),
                    onClick = onNavigateToHelp
                )
                DashboardActionCard(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.label_change_password),
                    onClick = onChangePassword
                )
                DashboardActionCard(
                    icon = Icons.Default.Logout,
                    title = stringResource(R.string.logout),
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onLogout
                )
                DashboardActionCard(
                    icon = Icons.Default.DeleteForever,
                    title = stringResource(R.string.label_delete_account),
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = onDeleteAccount
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProfileHero(user: Organization) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(248.dp)
            .background(Brush.verticalGradient(listOf(BrandTeal, BrandGreen)))
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (!user.profilePictureUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(user.profilePictureUrl).crossfade(true).build(),
                        contentDescription = stringResource(R.string.cd_profile_photo_of, user.name),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = when (user.type) {
                            OrganizationType.GROCERY -> Icons.Default.Store
                            OrganizationType.NGO -> Icons.Default.VolunteerActivism
                            OrganizationType.ADMIN -> Icons.Default.AdminPanelSettings
                        },
                        contentDescription = stringResource(R.string.cd_org_type, user.type.name.lowercase()),
                        modifier = Modifier.size(44.dp),
                        tint = Color.White
                    )
                }
            }

            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    label = when (user.type) {
                        OrganizationType.GROCERY -> stringResource(R.string.role_grocery)
                        OrganizationType.NGO -> stringResource(R.string.role_ngo)
                        OrganizationType.ADMIN -> stringResource(R.string.role_admin)
                    },
                    backgroundColor = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White
                )
                when (user.verificationStatus) {
                    VerificationStatus.APPROVED ->
                        StatusBadge(
                            label = stringResource(R.string.label_verified_badge),
                            backgroundColor = Color.White.copy(alpha = 0.22f),
                            contentColor = Color.White,
                            icon = Icons.Default.CheckCircle
                        )
                    VerificationStatus.PENDING ->
                        StatusBadge(
                            label = stringResource(R.string.label_pending_review_badge),
                            backgroundColor = Color(0xFFD97706).copy(alpha = 0.85f),
                            contentColor = Color.White,
                            icon = Icons.Default.Schedule
                        )
                    VerificationStatus.REJECTED ->
                        StatusBadge(
                            label = stringResource(R.string.label_rejected_badge),
                            backgroundColor = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            contentColor = Color.White,
                            icon = Icons.Default.Cancel
                        )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Change Password Dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword    by remember { mutableStateOf("") }
    var newPassword        by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var currentPwVisible   by remember { mutableStateOf(false) }
    var newPwVisible       by remember { mutableStateOf(false) }
    var error              by remember { mutableStateOf<String?>(null) }

    val errCurrentRequired  = stringResource(R.string.error_current_password_required)
    val errNewRequired      = stringResource(R.string.error_new_password_required)
    val errMinLength        = stringResource(R.string.error_password_min_length)
    val errNeedsUppercase   = stringResource(R.string.error_password_needs_uppercase)
    val errNeedsNumber      = stringResource(R.string.error_password_needs_number)
    val errDontMatch        = stringResource(R.string.error_passwords_dont_match)

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text(stringResource(R.string.label_change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value                = currentPassword,
                    onValueChange        = { currentPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_current_password)) },
                    visualTransformation = if (currentPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPwVisible = !currentPwVisible }) {
                            Icon(if (currentPwVisible) Icons.Default.Visibility
                                 else Icons.Default.VisibilityOff,
                                 if (currentPwVisible) stringResource(R.string.label_hide_password) else stringResource(R.string.label_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value                = newPassword,
                    onValueChange        = { newPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_new_password)) },
                    visualTransformation = if (newPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPwVisible = !newPwVisible }) {
                            Icon(if (newPwVisible) Icons.Default.Visibility
                                 else Icons.Default.VisibilityOff,
                                 if (newPwVisible) stringResource(R.string.label_hide_password) else stringResource(R.string.label_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                OutlinedTextField(
                    value                = confirmNewPassword,
                    onValueChange        = { confirmNewPassword = it; error = null },
                    label                = { Text(stringResource(R.string.label_confirm_new_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier             = Modifier.fillMaxWidth(), singleLine = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            ClearChainOutlinedButton(
                text = stringResource(R.string.action_change),
                enabled = !isLoading,
                onClick = {
                    when {
                        currentPassword.isBlank()             -> error = errCurrentRequired
                        newPassword.isBlank()                 -> error = errNewRequired
                        newPassword.length < 8                -> error = errMinLength
                        !newPassword.any { it.isUpperCase() } -> error = errNeedsUppercase
                        !newPassword.any { it.isDigit() }     -> error = errNeedsNumber
                        newPassword != confirmNewPassword      -> error = errDontMatch
                        else -> onConfirm(currentPassword, newPassword)
                    }
                },
                fillMaxWidth = false,
                loading = isLoading
            )
        },
        dismissButton = {
            ClearChainOutlinedButton(text = stringResource(R.string.cancel), onClick = onDismiss)
        }
    )
}

@Composable
private fun StatItem(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Account stats card
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AccountStatsCard(stats: com.clearchain.app.domain.model.OrgStats, orgType: OrganizationType) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (orgType == OrganizationType.GROCERY) {
                StatItem(stringResource(R.string.status_active), stats.activeListings.toString(), Icons.Default.Inventory)
                StatItem(stringResource(R.string.status_pending), stats.pendingRequests.toString(), Icons.Default.Pending)
                StatItem(stringResource(R.string.stat_completed), stats.completed.toString(), Icons.Default.CheckCircle)
                StatItem(stringResource(R.string.impact_food_saved), "${stats.foodSaved} kg", Icons.Default.Eco)
            } else {
                StatItem(stringResource(R.string.label_stat_requests), stats.totalCompleted.toString(), Icons.Default.LocalShipping)
                StatItem(stringResource(R.string.stat_in_stock), stats.inStock.toString(), Icons.Default.Inventory)
                StatItem(stringResource(R.string.stat_distributed), stats.distributed.toString(), Icons.Default.VolunteerActivism)
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Team Members Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TeamMembersCard(user: Organization) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MemberRow(
                name   = user.name,
                detail = user.email,
                badge  = stringResource(R.string.label_owner_badge),
                tint   = MaterialTheme.colorScheme.primaryContainer,
                onTint = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (!user.contactPerson.isNullOrBlank()) {
                HorizontalDivider()
                MemberRow(
                    name   = user.contactPerson,
                    detail = stringResource(R.string.label_contact_person),
                    badge  = stringResource(R.string.label_contact_badge),
                    tint   = MaterialTheme.colorScheme.secondaryContainer,
                    onTint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            HorizontalDivider()

            // Invite placeholder (multi-user support requires backend infrastructure)
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier         = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PersonAdd, null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.label_invite_team_member),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.label_coming_soon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    name: String,
    detail: String,
    badge: String,
    tint: Color,
    onTint: Color
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person, null,
                tint     = onTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Surface(shape = RoundedCornerShape(4.dp), color = tint) {
            Text(
                badge,
                style    = MaterialTheme.typography.labelSmall,
                color    = onTint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Delete Account dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DeleteAccountDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.label_delete_account), color = MaterialTheme.colorScheme.error) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.msg_delete_account_warning))
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it },
                    label         = { Text(stringResource(R.string.label_confirm_password)) },
                    singleLine    = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                 if (showPassword) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled  = !isLoading
                )
            }
        },
        confirmButton = {
            ClearChainButton(
                text = stringResource(R.string.label_delete_account),
                onClick = { if (password.isNotBlank()) onConfirm(password) },
                enabled = password.isNotBlank() && !isLoading,
                loading = isLoading,
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                fillMaxWidth = false
            )
        },
        dismissButton = {
            ClearChainOutlinedButton(
                text = stringResource(R.string.cancel),
                onClick = onDismiss,
                enabled = !isLoading
            )
        }
    )
}
