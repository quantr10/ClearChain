package com.clearchain.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.ButtonShape
import com.clearchain.app.util.HapticUtils

object ClearChainButtonDefaults {
    val Height = 32.dp
    val IconSize = 18.dp
    val Spacing = 6.dp
    val HorizontalPadding = 8.dp
}

@Composable
private fun clearChainButtonTextStyle(): TextStyle = MaterialTheme.typography.labelSmall

@Composable
fun ClearChainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    fillMaxWidth: Boolean = true,
    border: BorderStroke? = null,
    iconSize: Dp = ClearChainButtonDefaults.IconSize
) {
    val context = LocalContext.current
    Button(
        onClick = {
            if (!loading) {
                HapticUtils.confirm(context)
                onClick()
            }
        },
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier)
            .requiredHeight(ClearChainButtonDefaults.Height),
        enabled = enabled || loading,
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = ClearChainButtonDefaults.HorizontalPadding, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = border
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize)
                )
                Spacer(Modifier.width(ClearChainButtonDefaults.Spacing))
            }
            Text(
                text = text,
                style = clearChainButtonTextStyle()
            )
        }
    }
}

@Composable
fun ClearChainOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    fillMaxWidth: Boolean = false,
    border: BorderStroke? = null,
    iconSize: Dp = ClearChainButtonDefaults.IconSize
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            if (!loading) {
                HapticUtils.tick(context)
                onClick()
            }
        },
        modifier = (if (fillMaxWidth) modifier.fillMaxWidth() else modifier)
            .requiredHeight(ClearChainButtonDefaults.Height),
        enabled = enabled || loading,
        shape = ButtonShape,
        contentPadding = PaddingValues(horizontal = ClearChainButtonDefaults.HorizontalPadding, vertical = 0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        border = border ?: ButtonDefaults.outlinedButtonBorder(enabled)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(iconSize),
                color = contentColor,
                strokeWidth = 2.dp
            )
        } else {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(ClearChainButtonDefaults.Spacing))
            }
            Text(text = text, style = clearChainButtonTextStyle())
        }
    }
}

@Composable
fun ClearChainActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    Surface(
        onClick         = {
            if (enabled) {
                HapticUtils.tick(context)
                onClick()
            }
        },
        modifier        = modifier.size(24.dp),
        shape           = CircleShape,
        color           = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription,
                Modifier.size(18.dp),
                tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
