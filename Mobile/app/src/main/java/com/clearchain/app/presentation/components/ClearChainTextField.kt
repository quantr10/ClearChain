package com.clearchain.app.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.clearchain.app.ui.theme.ShapeMedium

@Composable
fun ClearChainTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "",
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    readOnly: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor = when {
        isError   -> MaterialTheme.colorScheme.error
        isFocused -> MaterialTheme.colorScheme.primary
        !enabled  -> MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        else      -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = 1.dp
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val iconTint     = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) {
            Text(
                text       = label,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color      = contentColor
            )
        }
        BasicTextField(
            value             = value,
            onValueChange     = onValueChange,
            modifier          = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(40.dp) else Modifier)
                .border(borderWidth, borderColor, ShapeMedium)
                .padding(horizontal = 12.dp)
                .then(if (!singleLine) Modifier.padding(vertical = 12.dp) else Modifier),
            textStyle         = MaterialTheme.typography.labelLarge.copy(color = contentColor),
            singleLine        = singleLine,
            minLines          = minLines,
            maxLines          = maxLines,
            readOnly          = readOnly,
            enabled           = enabled,
            interactionSource = interactionSource,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
                                   else VisualTransformation.None,
            keyboardOptions   = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions   = KeyboardActions(onAny = { onImeAction() }),
            decorationBox     = { innerTextField ->
                Row(
                    modifier              = if (singleLine) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                    verticalAlignment     = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, null, Modifier.size(18.dp), tint = iconTint)
                    }
                    Box(
                        modifier         = Modifier.weight(1f),
                        contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text  = placeholder,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                    when {
                        isPassword -> Box(
                            modifier         = Modifier
                                .size(24.dp)
                                .clickable(enabled = enabled) { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) stringResource(R.string.cd_hide_password)
                                                     else stringResource(R.string.cd_show_password),
                                modifier           = Modifier.size(18.dp),
                                tint               = iconTint
                            )
                        }
                        trailingIcon != null -> trailingIcon()
                        value.isNotEmpty() -> IconButton(
                            onClick  = { onValueChange("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Clear, stringResource(R.string.cd_clear_search), Modifier.size(18.dp), tint = iconTint)
                        }
                    }
                }
            }
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text     = errorMessage,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
