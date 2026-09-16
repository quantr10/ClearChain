package com.clearchain.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.ButtonShape
import com.clearchain.app.util.HapticUtils

@Composable
fun ClearChainQuantityStepper(
    quantity: Int,
    unit: String,
    canIncrement: Boolean,
    enabled: Boolean,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ClearChainButtonDefaults.Spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuantityStepperIconButton(
            icon = Icons.Default.Remove,
            contentDescription = stringResource(R.string.cart_decrease),
            onClick = onDecrement,
            tint = MaterialTheme.colorScheme.error,
            enabled = enabled
        )
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(ClearChainButtonDefaults.Height)
                .widthIn(min = 72.dp),
            shape = ButtonShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$quantity $unit",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
            }
        }
        QuantityStepperIconButton(
            icon = Icons.Default.Add,
            contentDescription = stringResource(R.string.cart_increase),
            onClick = onIncrement,
            tint = MaterialTheme.colorScheme.primary,
            enabled = enabled && canIncrement
        )
    }
}

@Composable
private fun QuantityStepperIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            if (enabled) {
                HapticUtils.tick(context)
                onClick()
            }
        },
        modifier = Modifier.size(ClearChainButtonDefaults.Height),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) {
                tint.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(ClearChainButtonDefaults.IconSize),
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}
