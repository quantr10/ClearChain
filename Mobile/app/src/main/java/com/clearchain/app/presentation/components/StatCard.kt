package com.clearchain.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.CardShape

@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    animated: Boolean = false
) {
    Card(
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = CardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accentColor
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (animated) {
                    val intValue = value.filter { it.isDigit() }.toIntOrNull() ?: 0
                    val suffix = value.filter { !it.isDigit() }
                    AnimatedCounter(
                        count = intValue,
                        style = MaterialTheme.typography.headlineSmall,
                        color = contentColor,
                        suffix = suffix
                    )
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatCardGrid(
    stats: List<Triple<ImageVector, String, String>>,
    accentColors: List<Color> = emptyList(),
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        stats.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEachIndexed { colIndex, (icon, label, value) ->
                    val statIndex = rowIndex * 2 + colIndex
                    val accent = accentColors.getOrNull(statIndex) ?: MaterialTheme.colorScheme.primary
                    StatCard(
                        icon = icon,
                        label = label,
                        value = value,
                        accentColor = accent,
                        modifier = Modifier.weight(1f),
                        animated = true
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
