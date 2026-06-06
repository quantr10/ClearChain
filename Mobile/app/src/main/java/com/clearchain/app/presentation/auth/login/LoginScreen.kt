package com.clearchain.app.presentation.auth.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.presentation.auth.AuthDivider
import com.clearchain.app.presentation.auth.AuthHeader
import com.clearchain.app.presentation.components.*
import com.clearchain.app.presentation.navigation.Screen
import com.clearchain.app.ui.theme.*
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var formVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { delay(150); formVisible = true }

    LaunchedEffect(true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.Navigate -> navController.navigate(event.route) {
                    if (event.route != Screen.Register.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                else -> Unit
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
            AuthHeader(subtitle = stringResource(R.string.welcome_back))

            AnimatedVisibility(
                visible = formVisible,
                enter   = fadeIn() + slideInVertically { it / 4 }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text       = stringResource(R.string.sign_in_to_account),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    ClearChainTextField(
                        value         = state.email,
                        onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
                        label         = stringResource(R.string.email_address),
                        placeholder   = stringResource(R.string.hint_email_you),
                        leadingIcon   = Icons.Default.Email,
                        keyboardType  = KeyboardType.Email,
                        imeAction     = ImeAction.Next,
                        isError       = state.emailError != null,
                        errorMessage  = state.emailError,
                        enabled       = !state.isLoading && !state.isLockedOut
                    )

                    ClearChainTextField(
                        value         = state.password,
                        onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
                        label         = stringResource(R.string.password),
                        placeholder   = stringResource(R.string.enter_password),
                        leadingIcon   = Icons.Default.Lock,
                        keyboardType  = KeyboardType.Password,
                        imeAction     = ImeAction.Done,
                        onImeAction   = { if (!state.isLockedOut) viewModel.onEvent(LoginEvent.Login) },
                        isPassword    = true,
                        isError       = state.passwordError != null,
                        errorMessage  = state.passwordError,
                        enabled       = !state.isLoading && !state.isLockedOut
                    )

                    // ── Remember me + Forgot password row ──────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Checkbox(
                                checked = state.rememberMe,
                                onCheckedChange = { viewModel.onEvent(LoginEvent.ToggleRememberMe) },
                                enabled = !state.isLoading
                            )
                            Text(
                                text  = stringResource(R.string.remember_me),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        TextButton(onClick = {}) {
                            Text(
                                text  = stringResource(R.string.forgot_password),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // ── Lockout / system error banner ──────────────────────
                    AnimatedVisibility(
                        visible = state.error != null || state.systemError != null,
                        enter   = fadeIn(),
                        exit    = fadeOut()
                    ) {
                        when {
                            state.isLockedOut -> AlertBanner(
                                message = stringResource(R.string.msg_account_locked, state.lockoutMinutes),
                                type    = AlertType.WARNING,
                                icon    = Icons.Default.Lock
                            )
                            state.systemError != null -> AlertBanner(
                                message = state.systemError.orEmpty(),
                                type    = AlertType.ERROR,
                                icon    = Icons.Default.ErrorOutline
                            )
                            else -> AlertBanner(
                                message = state.error.orEmpty(),
                                type    = AlertType.ERROR,
                                icon    = Icons.Default.ErrorOutline
                            )
                        }
                    }

                    ClearChainButton(
                        text    = stringResource(R.string.sign_in),
                        onClick = { viewModel.onEvent(LoginEvent.Login) },
                        loading = state.isLoading,
                        enabled = !state.isLoading && !state.isLockedOut
                                && state.email.isNotBlank() && state.password.isNotBlank()
                    )

                    AuthDivider()

                    ClearChainOutlinedButton(
                        text    = stringResource(R.string.create_new_account),
                        onClick = { navController.navigate(Screen.Register.route) },
                        enabled = !state.isLoading
                    )
                }
            }
        }
    }
}
