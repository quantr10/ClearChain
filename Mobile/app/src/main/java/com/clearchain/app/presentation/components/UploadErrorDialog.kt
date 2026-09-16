package com.clearchain.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.clearchain.app.R

@Composable
fun UploadErrorDialog(
    errorMessage: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onRetry,
        icon = Icons.Default.CloudOff,
        title = stringResource(R.string.label_upload_failed),
        message = errorMessage,
        confirmLabel = stringResource(R.string.action_retry_upload),
        confirmIcon = Icons.Default.Refresh,
        showConfirmButton = canRetry,
        dismissLabel = if (canRetry) stringResource(R.string.cancel) else stringResource(R.string.close)
    ) {
        if (!canRetry) {
            Text(
                text = stringResource(R.string.msg_try_again_later),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
