package com.clearchain.app.presentation.auth.verify

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.clearchain.app.R
import com.clearchain.app.presentation.components.ClearChainButton
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.ScreenPadding
import com.clearchain.app.util.UiEvent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun EmailVerificationScreen(
    navController: NavController,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
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

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.email_verify_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.email_verify_sent, state.email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = state.code,
                onValueChange = { viewModel.onEvent(EmailVerificationEvent.CodeChanged(it)) },
                singleLine = true,
                isError = state.codeError != null,
                supportingText = state.codeError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.width(240.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    letterSpacing = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            ClearChainButton(
                text = stringResource(R.string.submit),
                onClick = { viewModel.onEvent(EmailVerificationEvent.Verify) },
                enabled = state.code.length == 6,
                loading = state.isLoading
            )

            ClearChainButton(
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
                enabled = state.resendCooldownSeconds == 0,
                loading = state.isResending,
                containerColor = Color.White,
                contentColor = BrandGreen,
                border = BorderStroke(1.dp, BrandGreen)
            )
        }
    }
}
