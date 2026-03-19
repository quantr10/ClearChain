// ═══════════════════════════════════════════════════════════════════════════════
// ProfileScreen.kt — REDESIGNED with unified components and consistent layout
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.OrganizationType
import com.clearchain.app.presentation.components.*
import com.clearchain.app.util.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

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
                title = { Text(if (state.isEditing) "Edit Profile" else "Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isEditing) viewModel.onEvent(ProfileEvent.CancelEdit)
                        else onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (!state.isEditing) {
                        IconButton(
                            onClick = { viewModel.onEvent(ProfileEvent.StartEdit) },
                            enabled = !state.isLoading
                        ) {
                            Icon(Icons.Default.Edit, "Edit Profile")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading && state.user == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Profile Header ──────────────────────────────
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Avatar
                                Surface(
                                    modifier = Modifier.size(72.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            when (state.user!!.type) {
                                                OrganizationType.GROCERY -> Icons.Default.Store
                                                OrganizationType.NGO -> Icons.Default.VolunteerActivism
                                                OrganizationType.ADMIN -> Icons.Default.AdminPanelSettings
                                            },
                                            null,
                                            Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Text(
                                    state.user!!.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatusBadge(
                                        label = state.user!!.type.name,
                                        backgroundColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                    StatusBadge(
                                        label = "Verified",
                                        backgroundColor = MaterialTheme.colorScheme.tertiary,
                                        contentColor = MaterialTheme.colorScheme.onTertiary,
                                        icon = Icons.Default.CheckCircle
                                    )
                                }
                            }
                        }

                        // ── Error ────────────────────────────────────────
                        state.error?.let {
                            ErrorBanner(
                                message = it,
                                onDismiss = { viewModel.onEvent(ProfileEvent.ClearError) }
                            )
                        }

                        // ── Complete Profile Hint ────────────────────────
                        if (!state.isEditing &&
                            (state.user!!.phone.isBlank() || state.user!!.address.isBlank() || state.user!!.location.isBlank())) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                onClick = { viewModel.onEvent(ProfileEvent.StartEdit) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                    Column(Modifier.weight(1f)) {
                                        Text("Complete Your Profile",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text("Add contact details to get started",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    Icon(Icons.Default.ChevronRight, null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }

                        // ── Account Info ─────────────────────────────────
                        SectionHeader("Account Information")

                        if (state.isEditing) {
                            // ── EDIT MODE ────────────────────────────────
                            ClearChainTextField(
                                value = state.editName,
                                onValueChange = { viewModel.onEvent(ProfileEvent.EditNameChanged(it)) },
                                label = "Organization Name",
                                leadingIcon = { Icon(Icons.Default.Business, null) },
                                imeAction = ImeAction.Next,
                                isError = state.editNameError != null,
                                errorMessage = state.editNameError,
                                enabled = !state.isSavingProfile
                            )

                            ProfileInfoRow(Icons.Default.Email, "Email", state.user!!.email)

                            ClearChainTextField(
                                value = state.editPhone,
                                onValueChange = { viewModel.onEvent(ProfileEvent.EditPhoneChanged(it)) },
                                label = "Phone Number",
                                placeholder = "+1-234-567-8900",
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next,
                                isError = state.editPhoneError != null,
                                errorMessage = state.editPhoneError,
                                enabled = !state.isSavingProfile
                            )

                            ClearChainTextField(
                                value = state.editAddress,
                                onValueChange = { viewModel.onEvent(ProfileEvent.EditAddressChanged(it)) },
                                label = "Address",
                                placeholder = "123 Main Street",
                                leadingIcon = { Icon(Icons.Default.Home, null) },
                                imeAction = ImeAction.Next,
                                enabled = !state.isSavingProfile
                            )

                            ClearChainTextField(
                                value = state.editLocation,
                                onValueChange = { viewModel.onEvent(ProfileEvent.EditLocationChanged(it)) },
                                label = "City/Location",
                                placeholder = "New York, NY",
                                leadingIcon = { Icon(Icons.Default.Place, null) },
                                imeAction = if (state.user!!.type == OrganizationType.GROCERY) ImeAction.Next else ImeAction.Done,
                                enabled = !state.isSavingProfile
                            )

                            if (state.user!!.type == OrganizationType.GROCERY) {
                                ClearChainTextField(
                                    value = state.editHours,
                                    onValueChange = { viewModel.onEvent(ProfileEvent.EditHoursChanged(it)) },
                                    label = "Operating Hours",
                                    placeholder = "9AM – 9PM",
                                    leadingIcon = { Icon(Icons.Default.Schedule, null) },
                                    imeAction = ImeAction.Done,
                                    enabled = !state.isSavingProfile
                                )
                            }

                            // Save / Cancel
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.onEvent(ProfileEvent.CancelEdit) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !state.isSavingProfile,
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("Cancel") }

                                ClearChainButton(
                                    text = "Save Changes",
                                    onClick = { viewModel.onEvent(ProfileEvent.SaveProfile) },
                                    loading = state.isSavingProfile,
                                    enabled = !state.isSavingProfile,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // ── READ-ONLY MODE ──────────────────────────
                            ProfileInfoRow(Icons.Default.Email, "Email", state.user!!.email)
                            ProfileInfoRow(Icons.Default.Phone, "Phone", state.user!!.phone.ifBlank { "Not provided" })
                            ProfileInfoRow(Icons.Default.Home, "Address", state.user!!.address.ifBlank { "Not provided" })
                            ProfileInfoRow(Icons.Default.Place, "Location", state.user!!.location.ifBlank { "Not provided" })
                            if (state.user!!.type == OrganizationType.GROCERY) {
                                ProfileInfoRow(Icons.Default.Schedule, "Hours", state.user!!.hours ?: "Not provided")
                            }
                        }

                        HorizontalDivider()

                        // ── Change Password ─────────────────────────────
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !state.isEditing
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Change Password")
                        }

                        // ── Logout ──────────────────────────────────────
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            enabled = !state.isEditing
                        ) {
                            Icon(Icons.Default.Logout, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Logout")
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                else -> {
                    EmptyState(
                        icon = Icons.Default.ErrorOutline,
                        title = "Failed to load profile",
                        actionLabel = "Retry",
                        onAction = { viewModel.onEvent(ProfileEvent.LoadProfile) }
                    )
                }
            }
        }
    }

    // ── Dialogs ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        ConfirmDialog(
            title = "Logout?",
            message = "Are you sure you want to logout?",
            confirmLabel = "Logout",
            isDestructive = true,
            onConfirm = { showLogoutDialog = false; onLogout() },
            onDismiss = { showLogoutDialog = false }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, new_ ->
                viewModel.onEvent(ProfileEvent.ChangePassword(current, new_))
                showChangePasswordDialog = false
            }
        )
    }
}

// ── Profile Info Row (read-only, card-style) ────────────────────────────────

@Composable
private fun ProfileInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Change Password Dialog ──────────────────────────────────────────────────

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var currentPwVisible by remember { mutableStateOf(false) }
    var newPwVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; error = null },
                    label = { Text("Current Password") },
                    visualTransformation = if (currentPwVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPwVisible = !currentPwVisible }) {
                            Icon(if (currentPwVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("New Password") },
                    visualTransformation = if (newPwVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPwVisible = !newPwVisible }) {
                            Icon(if (newPwVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it; error = null },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    when {
                        currentPassword.isBlank() -> error = "Current password is required"
                        newPassword.isBlank() -> error = "New password is required"
                        newPassword.length < 8 -> error = "At least 8 characters"
                        !newPassword.any { it.isUpperCase() } -> error = "Needs uppercase letter"
                        !newPassword.any { it.isDigit() } -> error = "Needs a number"
                        newPassword != confirmNewPassword -> error = "Passwords don't match"
                        else -> onConfirm(currentPassword, newPassword)
                    }
                }
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(16.dp))
                else Text("Change")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}