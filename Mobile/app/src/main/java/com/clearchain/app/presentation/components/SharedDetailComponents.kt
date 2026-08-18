package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.CardShape

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(top = 1.dp),
            tint = iconTint
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = valueColor
            )
        }
    }
}

@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DashboardSection(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContentColor = contentColorFor(containerColor)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = resolvedContentColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (title.isNotBlank() || action != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = resolvedContentColor
                        )
                    }
                    action?.invoke()
                }
            }
            content()
        }
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = containerColor,
        shape = CardShape,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Shows state-backed feedback through the app's single Material snackbar pattern. */
@Composable
fun SnackbarMessageEffect(
    snackbarHostState: SnackbarHostState,
    message: String?,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message = message, duration = duration)
        }
    }
}
