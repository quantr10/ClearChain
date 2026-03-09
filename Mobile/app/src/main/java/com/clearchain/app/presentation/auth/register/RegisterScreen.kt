package com.clearchain.app.presentation.auth.register

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainTextField
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.util.UiEvent
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // ✅ NEW: Request notification permission (Android 13+)
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    // ✅ NEW: Request permission on first composition
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (notificationPermissionState?.status?.isGranted == false) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is UiEvent.NavigateUp -> {
                    navController.navigateUp()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Text(
                text = "🍎",
                style = MaterialTheme.typography.displayMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Join ClearChain",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Help reduce food waste together",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Organization Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Organization Type",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.type == "grocery",
                            onClick = { viewModel.onEvent(RegisterEvent.TypeChanged("grocery")) },
                            label = { Text("Grocery Store") },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        )

                        FilterChip(
                            selected = state.type == "ngo",
                            onClick = { viewModel.onEvent(RegisterEvent.TypeChanged("ngo")) },
                            label = { Text("NGO") },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isLoading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Organization Name
            ClearChainTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(RegisterEvent.NameChanged(it)) },
                label = "Organization Name",
                placeholder = "Your organization name",
                leadingIcon = { Icon(Icons.Default.Person, null) },
                imeAction = ImeAction.Next,
                isError = state.nameError != null,
                errorMessage = state.nameError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email
            ClearChainTextField(
                value = state.email,
                onValueChange = { viewModel.onEvent(RegisterEvent.EmailChanged(it)) },
                label = "Email",
                placeholder = "organization@example.com",
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                isError = state.emailError != null,
                errorMessage = state.emailError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phone
            ClearChainTextField(
                value = state.phone,
                onValueChange = { viewModel.onEvent(RegisterEvent.PhoneChanged(it)) },
                label = "Phone Number",
                placeholder = "+1-234-567-8900",
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                isError = state.phoneError != null,
                errorMessage = state.phoneError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            ClearChainTextField(
                value = state.password,
                onValueChange = { viewModel.onEvent(RegisterEvent.PasswordChanged(it)) },
                label = "Password",
                placeholder = "Create a strong password",
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                isPassword = true,
                isError = state.passwordError != null,
                errorMessage = state.passwordError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password
            ClearChainTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
                label = "Confirm Password",
                placeholder = "Re-enter your password",
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
                isPassword = true,
                isError = state.confirmPasswordError != null,
                errorMessage = state.confirmPasswordError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Address
            ClearChainTextField(
                value = state.address,
                onValueChange = { viewModel.onEvent(RegisterEvent.AddressChanged(it)) },
                label = "Address",
                placeholder = "123 Main Street",
                leadingIcon = { Icon(Icons.Default.Home, null) },
                imeAction = ImeAction.Next,
                isError = state.addressError != null,
                errorMessage = state.addressError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Location/City
            ClearChainTextField(
                value = state.location,
                onValueChange = { viewModel.onEvent(RegisterEvent.LocationChanged(it)) },
                label = "City/Location",
                placeholder = "New York, NY",
                leadingIcon = { Icon(Icons.Default.Place, null) },
                imeAction = if (state.type == "grocery") ImeAction.Next else ImeAction.Done,
                isError = state.locationError != null,
                errorMessage = state.locationError,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Hours (optional, grocery only)
            if (state.type == "grocery") {
                ClearChainTextField(
                    value = state.hours,
                    onValueChange = { viewModel.onEvent(RegisterEvent.HoursChanged(it)) },
                    label = "Operating Hours (Optional)",
                    placeholder = "9AM - 9PM",
                    leadingIcon = null,
                    imeAction = ImeAction.Done,
                    onImeAction = { viewModel.onEvent(RegisterEvent.Register) },
                    enabled = !state.isLoading
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Register button
            ClearChainButton(
                text = "Create Account",
                onClick = { viewModel.onEvent(RegisterEvent.Register) },
                loading = state.isLoading,
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Already have account
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account?",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = { navController.navigateUp() },
                    enabled = !state.isLoading
                ) {
                    Text("Sign In")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            if (state.error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error!!,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}