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

    /** Disabled buttons keep their own hue at this opacity so a red action still reads as red. */
    const val DisabledAlpha = 0.38f
}

@Composable
private fun clearChainButtonTextStyle(): TextStyle = MaterialTheme.typography.labelSmall

/**
 * Filled button: the one primary action of a screen, section, sheet or dialog
 * (submit, save, approve, apply filters, confirm). Pass `containerColor = error` only
 * when that primary action is itself destructive (delete).
 *
 * Everything else — cancel/back, navigation ("view more", "view document"), alternatives
 * that sit beside the primary action — uses [ClearChainOutlinedButton].
 */
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
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = ClearChainButtonDefaults.DisabledAlpha),
            disabledContentColor = contentColor
        ),
        border = border
    ) {
        if (loading) {
            InlineSpinner(color = contentColor)
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

/**
 * Outlined button: secondary actions. Use `contentColor = error` (border stays neutral)
 * for destructive secondaries such as reject, cancel request, clear notifications or log out
 * (resetting filters is not destructive and stays primary-coloured).
 */
@Composable
fun ClearChainOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
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
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor.copy(alpha = ClearChainButtonDefaults.DisabledAlpha)
        ),
        border = border ?: ButtonDefaults.outlinedButtonBorder(enabled || loading)
    ) {
        if (loading) {
            InlineSpinner(color = contentColor)
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
    enabled: Boolean = true,
    /** Its action is in flight: the icon becomes a spinner and taps are ignored. */
    loading: Boolean = false
) {
    val context = LocalContext.current
    Surface(
        onClick = {
            if (enabled && !loading) {
                HapticUtils.tick(context)
                onClick()
            }
        },
        modifier = modifier.size(24.dp),
        shape = CircleShape,
        color = if (enabled) containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                InlineSpinner(color = tint)
            } else {
                Icon(
                    icon,
                    contentDescription,
                    Modifier.size(18.dp),
                    tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
