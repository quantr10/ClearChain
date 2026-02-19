package com.clearchain.app.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.clearchain.app.domain.model.OrganizationType
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
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.user != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ── Profile Header ──────────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = when (state.user!!.type) {
                                                OrganizationType.GROCERY -> Icons.Default.Store
                                                OrganizationType.NGO     -> Icons.Default.VolunteerActivism
                                                OrganizationType.ADMIN   -> Icons.Default.AdminPanelSettings
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = state.user!!.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = when (state.user!!.type) {
                                            OrganizationType.GROCERY -> "GROCERY"
                                            OrganizationType.NGO     -> "NGO"
                                            OrganizationType.ADMIN   -> "ADMIN"
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        // ── Error card ──────────────────────────────────────
                        state.error?.let { error ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = error,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { viewModel.onEvent(ProfileEvent.ClearError) }) {
                                        Icon(
                                            Icons.Default.Close, "Dismiss",
                                            tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }

                        // ── Account Information ─────────────────────────────
                        Text(
                            text = "Account Information",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        ProfileInfoCard(
                            icon  = Icons.Default.Email,
                            label = "Email",
                            value = state.user!!.email
                        )

                        ProfileInfoCard(
                            icon  = Icons.Default.Phone,
                            label = "Phone",
                            value = state.user!!.phone.ifBlank { "Not provided" }
                        )

                        ProfileInfoCard(
                            icon  = Icons.Default.LocationOn,
                            label = "Location",
                            value = state.user!!.location.ifBlank { "Not provided" }
                        )

                        ProfileInfoCard(
                            icon  = Icons.Default.Home,
                            label = "Address",
                            value = state.user!!.address.ifBlank { "Not provided" }
                        )

                        if (state.user!!.type == OrganizationType.GROCERY) {
                            ProfileInfoCard(
                                icon  = Icons.Default.Schedule,
                                label = "Hours",
                                value = state.user!!.hours ?: "Not provided"
                            )
                        }

                        // ── Verification Status ─────────────────────────────
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.user!!.verified)
                                    MaterialTheme.colorScheme.tertiaryContainer
                                else
                                    MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (state.user!!.verified) Icons.Default.CheckCircle
                                                  else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (state.user!!.verified)
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Column {
                                    Text(
                                        text = "Verification Status",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (state.user!!.verified)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = if (state.user!!.verified) "Verified" else "Pending Verification",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (state.user!!.verified)
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        else
                                            MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // ── Change Password Button ──────────────────────────
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change Password")
                        }

                        // ── Logout Button ───────────────────────────────────
                        Button(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                }

                else -> {
                    // Error / empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Failed to load profile", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.onEvent(ProfileEvent.LoadProfile) }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // ── Logout Confirmation Dialog ──────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout?") },
            text  = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Logout") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Change Password Dialog ──────────────────────────────────────────────────
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            isLoading = state.isChangingPassword,
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                viewModel.onEvent(ProfileEvent.ChangePassword(currentPassword, newPassword))
                showChangePasswordDialog = false
            }
        )
    }
}

// ─── ProfileInfoCard ────────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null,
                 tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(text = label,
                     style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value,
                     style = MaterialTheme.typography.bodyLarge,
                     fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ─── ChangePasswordDialog ────────────────────────────────────────────────────────

@Composable
private fun ChangePasswordDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword      by remember { mutableStateOf("") }
    var newPassword          by remember { mutableStateOf("") }
    var confirmNewPassword   by remember { mutableStateOf("") }
    var currentPwVisible     by remember { mutableStateOf(false) }
    var newPwVisible         by remember { mutableStateOf(false) }
    var confirmPwVisible     by remember { mutableStateOf(false) }
    var validationError      by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; validationError = null },
                    label = { Text("Current Password") },
                    visualTransformation = if (currentPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentPwVisible = !currentPwVisible }) {
                            Icon(
                                if (currentPwVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; validationError = null },
                    label = { Text("New Password") },
                    visualTransformation = if (newPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPwVisible = !newPwVisible }) {
                            Icon(
                                if (newPwVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Confirm New Password
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it; validationError = null },
                    label = { Text("Confirm New Password") },
                    visualTransformation = if (confirmPwVisible) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPwVisible = !confirmPwVisible }) {
                            Icon(
                                if (confirmPwVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Validation Error
                validationError?.let {
                    Text(text = it,
                         color = MaterialTheme.colorScheme.error,
                         style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isLoading,
                onClick = {
                    when {
                        currentPassword.isBlank()    -> validationError = "Current password is required"
                        newPassword.isBlank()         -> validationError = "New password is required"
                        newPassword.length < 8        -> validationError = "Password must be at least 8 characters"
                        !newPassword.any { it.isUpperCase() } -> validationError = "Password must contain uppercase letter"
                        !newPassword.any { it.isLowerCase() } -> validationError = "Password must contain lowercase letter"
                        !newPassword.any { it.isDigit() }     -> validationError = "Password must contain a number"
                        newPassword != confirmNewPassword      -> validationError = "Passwords do not match"
                        else -> onConfirm(currentPassword, newPassword)
                    }
                }
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Change")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}