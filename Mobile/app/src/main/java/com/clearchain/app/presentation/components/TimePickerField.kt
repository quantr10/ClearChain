package com.clearchain.app.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.ShapeMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerField(
    value: String,
    onTimeSelected: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    fieldHeight: Dp = 32.dp,
    fieldShape: Shape = ShapeMedium
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialHour = value.takeIf { it.length >= 5 }?.substring(0, 2)?.toIntOrNull() ?: 0
    val initialMinute = value.takeIf { it.length >= 5 }?.substring(3, 5)?.toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    val borderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant
    val contentColor = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val iconTint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (label.isNotEmpty()) {
            OptionalFieldLabel(
                text = label,
                isOptional = isOptional,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .border(1.dp, borderColor, fieldShape)
                .clickable(enabled = enabled) { showPicker = true }
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = iconTint)
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (value.isNotBlank()) contentColor
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                if (value.isNotBlank()) {
                    IconButton(
                        onClick = { onTimeSelected("") },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            stringResource(R.string.cd_clear_search),
                            Modifier.size(14.dp),
                            tint = iconTint
                        )
                    }
                }
            }
        }
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (showPicker) {
        ConfirmDialog(
            onDismiss = { showPicker = false },
            icon = Icons.Default.AccessTime,
            title = stringResource(R.string.label_select_time),
            message = stringResource(R.string.msg_select_time),
            confirmLabel = stringResource(R.string.ok),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = {
                val h = timePickerState.hour.toString().padStart(2, '0')
                val m = timePickerState.minute.toString().padStart(2, '0')
                onTimeSelected("$h:$m")
                showPicker = false
            }
        ) {
            TimePicker(state = timePickerState)
        }
    }
}
