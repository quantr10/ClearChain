package com.clearchain.app.presentation.auth.forgot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.presentation.auth.AuthHeader
import com.clearchain.app.presentation.components.BlockBackWhile
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainOutlinedButton
import com.clearchain.app.presentation.components.ClearChainTextField
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent

/** Back-stack result key: the email whose password was just reset, read by LoginScreen. */
const val PASSWORD_RESET_EMAIL_KEY = "password_reset_email"

@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BlockBackWhile(state.isBusy)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                UiEvent.NavigateUp -> {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(PASSWORD_RESET_EMAIL_KEY, viewModel.state.value.email)
                    navController.navigateUp()
                }
                else -> Unit
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AuthHeader(
                subtitle = stringResource(R.string.forgot_password),
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        enabled = !state.isBusy
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                }
            )

            Column(
                modifier = Modifier.padding(ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.reset_password_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!state.codeSent) {
                    EmailStep(state = state, onEvent = viewModel::onEvent)
                } else {
                    ResetStep(state = state, onEvent = viewModel::onEvent)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.EmailStep(
    state: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit
) {
    Text(
        text = stringResource(R.string.reset_password_email_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ClearChainTextField(
        value = state.email,
        onValueChange = { onEvent(ForgotPasswordEvent.EmailChanged(it)) },
        label = stringResource(R.string.email_address),
        placeholder = stringResource(R.string.hint_email_you),
        leadingIcon = Icons.Default.Email,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Done,
        onImeAction = { onEvent(ForgotPasswordEvent.SendCode) },
        isError = state.emailError != null,
        errorMessage = state.emailError,
        enabled = !state.isSending
    )

    ClearChainButton(
        text = stringResource(R.string.reset_password_send_code),
        onClick = { onEvent(ForgotPasswordEvent.SendCode) },
        loading = state.isSending,
        enabled = state.email.isNotBlank()
    )
}

@Composable
private fun ColumnScope.ResetStep(
    state: ForgotPasswordState,
    onEvent: (ForgotPasswordEvent) -> Unit
) {
    Text(
        text = stringResource(R.string.reset_password_code_intro, state.email),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    ClearChainTextField(
        value = state.code,
        onValueChange = { onEvent(ForgotPasswordEvent.CodeChanged(it)) },
        label = stringResource(R.string.reset_password_code_label),
        leadingIcon = Icons.Default.Pin,
        keyboardType = KeyboardType.NumberPassword,
        isError = state.codeError != null,
        errorMessage = state.codeError,
        enabled = !state.isBusy
    )

    ClearChainTextField(
        value = state.newPassword,
        onValueChange = { onEvent(ForgotPasswordEvent.NewPasswordChanged(it)) },
        label = stringResource(R.string.label_new_password),
        placeholder = stringResource(R.string.password_placeholder),
        leadingIcon = Icons.Default.Lock,
        keyboardType = KeyboardType.Password,
        isPassword = true,
        isError = state.newPasswordError != null,
        errorMessage = state.newPasswordError,
        enabled = !state.isBusy
    )

    ClearChainTextField(
        value = state.confirmPassword,
        onValueChange = { onEvent(ForgotPasswordEvent.ConfirmPasswordChanged(it)) },
        label = stringResource(R.string.confirm_password),
        placeholder = stringResource(R.string.confirm_password_placeholder),
        leadingIcon = Icons.Default.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done,
        onImeAction = { onEvent(ForgotPasswordEvent.ResetPassword) },
        isPassword = true,
        isError = state.confirmPasswordError != null,
        errorMessage = state.confirmPasswordError,
        enabled = !state.isBusy
    )

    ClearChainButton(
        text = stringResource(R.string.reset_password_submit),
        onClick = { onEvent(ForgotPasswordEvent.ResetPassword) },
        loading = state.isResetting,
        enabled = !state.isSending && state.code.length == 6 &&
            state.newPassword.isNotBlank() && state.confirmPassword.isNotBlank()
    )

    ClearChainOutlinedButton(
        text = if (state.resendCooldownSeconds > 0) {
            stringResource(R.string.email_resend_cooldown, state.resendCooldownSeconds)
        } else {
            stringResource(R.string.email_resend_label)
        },
        onClick = { onEvent(ForgotPasswordEvent.ResendCode) },
        enabled = state.resendCooldownSeconds == 0 && !state.isResetting,
        loading = state.isSending,
        fillMaxWidth = true
    )

    TextButton(
        onClick = { onEvent(ForgotPasswordEvent.ChangeEmail) },
        enabled = !state.isBusy,
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text(stringResource(R.string.reset_password_change_email))
    }
}
