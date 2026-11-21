package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.clearchain.app.ui.theme.EmptyActionMinWidth
import com.clearchain.app.ui.theme.EmptyStatePadding
import com.clearchain.app.ui.theme.ListGap
import com.clearchain.app.ui.theme.ScreenHPadding

/**
 * What an area shows when it has nothing to show: the status picture, a title, an optional
 * subtitle and an optional button.
 *
 * In an area of bounded height it is centred when it fits and scrolls when it doesn't, so
 * large text never clips. Inside a parent that already scrolls (unbounded height) it lays out
 * at its natural height and leaves scrolling to that parent.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val fill = if (constraints.hasBoundedHeight) {
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
        } else {
            Modifier
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ScreenHPadding, Alignment.CenterVertically),
            modifier = Modifier
                .fillMaxWidth()
                .then(fill)
                .padding(EmptyStatePadding)
        ) {
            StatusDisc(icon)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ListGap)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (actionLabel != null && onAction != null) {
                ClearChainButton(
                    text = actionLabel,
                    onClick = onAction,
                    modifier = Modifier.widthIn(min = EmptyActionMinWidth),
                    fillMaxWidth = false
                )
            }
        }
    }
}
