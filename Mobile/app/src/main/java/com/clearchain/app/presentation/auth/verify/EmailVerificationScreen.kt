package com.clearchain.app.presentation.auth.verify

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.presentation.components.BlockBackWhile
import com.clearchain.app.presentation.components.BrandHeader
import com.clearchain.app.presentation.components.BrandScaffold
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.presentation.components.ClearChainOutlinedButton
import com.clearchain.app.presentation.components.CodeInputField
import com.clearchain.app.presentation.components.StatusDisc
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    BlockBackWhile(state.isLoading || state.isResending)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is UiEvent.Navigate -> {
                    navController.navigate(event.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                UiEvent.NavigateUp -> navController.navigateUp()
                is UiEvent.ShareFile -> { }
            }
        }
    }

    BrandScaffold(
        header = BrandHeader.Compact(title = stringResource(R.string.email_verify_title)),
        snackbarHostState = snackbarHostState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ListGap)
        ) {
            StatusDisc(Icons.Default.MarkEmailUnread)

            Text(
                text = stringResource(R.string.email_verify_sent, state.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            CodeInputField(
                value = state.code,
                onValueChange = { viewModel.onEvent(EmailVerificationEvent.CodeChanged(it)) },
                label = stringResource(R.string.verify_code_label),
                showLabel = false,
                isError = state.codeError != null,
                errorMessage = state.codeError,
                enabled = !state.isLoading,
                imeAction = ImeAction.Done,
                onImeAction = {
                    if (state.code.length == 6 && !state.isResending) {
                        viewModel.onEvent(EmailVerificationEvent.Verify)
                    }
                }
            )

            ClearChainButton(
                text = stringResource(R.string.action_verify),
                onClick = { viewModel.onEvent(EmailVerificationEvent.Verify) },
                enabled = state.code.length == 6 && !state.isResending,
                loading = state.isLoading
            )

            Text(
                text = stringResource(R.string.verify_code_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            ClearChainOutlinedButton(
                text = when {
                    state.resendCooldownSeconds > 0 ->
                        stringResource(R.string.email_resend_cooldown, state.resendCooldownSeconds)
                    else -> stringResource(R.string.email_resend_label)
                },
                onClick = {
                    if (!state.isResending) {
                        viewModel.onEvent(EmailVerificationEvent.ResendCode)
                    }
                },
                enabled = state.resendCooldownSeconds == 0 && !state.isLoading,
                loading = state.isResending,
                fillMaxWidth = true
            )
        }
    }
}
