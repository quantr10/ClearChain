package com.clearchain.app.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ScreenPadding
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
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }

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
                        onChangeAvatar = { showAvatarPickerDialog = true },
                        onChangePassword = { showChangePasswordDialog = true },
                        onLogout = { showLogoutDialog = true },
                        onDeleteAccount = { showDeleteAccountDialog = true },
                        onNavigateToAnalytics = onNavigateToAnalytics,
                        onNavigateToHelp = onNavigateToHelp,
                        onNavigateToAccountDetail = onNavigateToAccountDetail,
                        onNavigateToSettings = onNavigateToSettings
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
        LogoutDialog(
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

    if (showAvatarPickerDialog) {
        PhotoPickerDialog(
            onPhotoSelected = { uri ->
                viewModel.onEvent(ProfileEvent.AvatarSelected(uri))
                showAvatarPickerDialog = false
            },
            onDismiss = { showAvatarPickerDialog = false },
            title = stringResource(R.string.label_change_avatar),
            message = stringResource(R.string.msg_avatar_source),
            previewMessage = stringResource(R.string.msg_use_this_avatar)
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
    onChangeAvatar: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToAccountDetail: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val user = state.user!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHero(
            user = user,
            isUploadingAvatar = state.isUploadingAvatar,
            onChangeAvatar = onChangeAvatar
        )

        Column(
            modifier = Modifier.padding(ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardActionCard(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.account_detail),
                    onClick = onNavigateToAccountDetail
                )
                // Admins have their own Statistics tab; the analytics screen reports
                // reputation and pickup figures that only apply to an NGO or grocery.
                if (user.type != OrganizationType.ADMIN) {
                    DashboardActionCard(
                        icon = Icons.Default.BarChart,
                        title = stringResource(R.string.label_analytics),
                        onClick = onNavigateToAnalytics
                    )
                }
                DashboardActionCard(
                    icon = Icons.Default.HelpOutline,
                    title = stringResource(R.string.label_help_faq),
                    onClick = onNavigateToHelp
                )
                DashboardActionCard(
                    icon = Icons.Default.Settings,
                    title = stringResource(R.string.settings),
                    onClick = onNavigateToSettings
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
private fun ProfileHero(
    user: Organization,
    isUploadingAvatar: Boolean = false,
    onChangeAvatar: () -> Unit = {}
) {
    val context = LocalContext.current
    val changeAvatarLabel = stringResource(R.string.cd_change_avatar)
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
            // Tapping the avatar (or the role icon standing in for it) opens the
            // photo picker; every role owns a profile picture, so it is always enabled.
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(
                            enabled = !isUploadingAvatar,
                            onClickLabel = changeAvatarLabel,
                            onClick = onChangeAvatar
                        ),
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

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = 4.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(
                            enabled = !isUploadingAvatar,
                            onClickLabel = changeAvatarLabel,
                            onClick = onChangeAvatar
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = changeAvatarLabel,
                        modifier = Modifier.size(16.dp),
                        tint = BrandTeal
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
    var error              by remember { mutableStateOf<String?>(null) }

    val errCurrentRequired  = stringResource(R.string.error_current_password_required)
    val errNewRequired      = stringResource(R.string.error_new_password_required)
    val errMinLength        = stringResource(R.string.error_password_min_length)
    val errNeedsUppercase   = stringResource(R.string.error_password_needs_uppercase)
    val errNeedsNumber      = stringResource(R.string.error_password_needs_number)
    val errDontMatch        = stringResource(R.string.error_passwords_dont_match)

    ConfirmDialog(
        onDismiss = onDismiss,
        icon = Icons.Default.Lock,
        title = stringResource(R.string.label_change_password),
        message = stringResource(R.string.msg_change_password),
        confirmLabel = stringResource(R.string.action_change),
        dismissLabel = stringResource(R.string.cancel),
        confirmEnabled = !isLoading,
        confirmLoading = isLoading,
        onConfirm = {
            when {
                currentPassword.isBlank()             -> error = errCurrentRequired
                newPassword.isBlank()                 -> error = errNewRequired
                newPassword.length < 8                -> error = errMinLength
                !newPassword.any { it.isUpperCase() } -> error = errNeedsUppercase
                !newPassword.any { it.isDigit() }     -> error = errNeedsNumber
                newPassword != confirmNewPassword      -> error = errDontMatch
                else -> onConfirm(currentPassword, newPassword)
            }
        }
    ) {
        ClearChainTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it; error = null },
            label = stringResource(R.string.label_current_password),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        ClearChainTextField(
            value = newPassword,
            onValueChange = { newPassword = it; error = null },
            label = stringResource(R.string.label_new_password),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        ClearChainTextField(
            value = confirmNewPassword,
            onValueChange = { confirmNewPassword = it; error = null },
            label = stringResource(R.string.label_confirm_new_password),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Account stats card
// ═══════════════════════════════════════════════════════════════════════════════

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
// Logout dialog
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LogoutDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = stringResource(R.string.logout),
        message = stringResource(R.string.msg_logout_confirm),
        confirmLabel = stringResource(R.string.logout),
        dismissLabel = stringResource(R.string.cancel),
        isDestructive = true,
        icon = Icons.Default.Logout
    )
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

    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = { if (password.isNotBlank()) onConfirm(password) },
        icon = Icons.Default.DeleteForever,
        title = stringResource(R.string.label_delete_account),
        message = stringResource(R.string.msg_delete_account_warning),
        confirmLabel = stringResource(R.string.label_delete_account),
        dismissLabel = stringResource(R.string.cancel),
        isDestructive = true,
        confirmEnabled = password.isNotBlank() && !isLoading,
        confirmLoading = isLoading,
        dismissEnabled = !isLoading,
        dismissible = !isLoading
    ) {
        ClearChainTextField(
            value         = password,
            onValueChange = { password = it },
            label         = stringResource(R.string.label_confirm_password),
            keyboardType  = KeyboardType.Password,
            imeAction     = ImeAction.Done,
            isPassword    = true,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            modifier      = Modifier.fillMaxWidth(),
            enabled       = !isLoading
        )
    }
}
