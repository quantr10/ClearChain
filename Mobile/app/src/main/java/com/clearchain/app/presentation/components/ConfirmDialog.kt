package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.clearchain.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ShapeLarge
import com.clearchain.app.util.HapticUtils

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "",
    dismissLabel: String = "",
    isDestructive: Boolean = false,
    icon: ImageVector? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val resolvedConfirm = confirmLabel.ifEmpty { stringResource(R.string.btn_confirm) }
    val resolvedDismiss = dismissLabel.ifEmpty { stringResource(R.string.btn_cancel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ShapeLarge,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (isDestructive) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isDestructive) HapticUtils.warning(context) else HapticUtils.confirm(context)
                    onConfirm()
                },
                colors = if (isDestructive) ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) else ButtonDefaults.buttonColors()
            ) {
                Text(resolvedConfirm)
            }
        },
        dismissButton = {
            TextButton(onClick = { HapticUtils.tick(context); onDismiss() }) {
                Text(resolvedDismiss)
            }
        }
    )
}

@Composable
fun DestructiveConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val resolvedLabel = confirmLabel.ifEmpty { stringResource(R.string.btn_delete) }
    ConfirmDialog(
        title = title,
        message = message,
        confirmLabel = resolvedLabel,
        isDestructive = true,
        icon = Icons.Default.Warning,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}
