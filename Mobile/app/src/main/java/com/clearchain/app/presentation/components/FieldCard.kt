package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.CardPadding

/**
 * The label of the [FieldCard] around a field, or null outside one. Fields with no label of their
 * own use it to name their clear button.
 */
val LocalFieldLabel: ProvidableCompositionLocal<String?> = staticCompositionLocalOf { null }

/**
 * One form field in its own card, with a small label above the input. A "*" in [label] is
 * dropped: [isOptional] is what marks a field as optional.
 */
@Composable
fun FieldCard(
    label: String,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val shownLabel = label.replace("*", "").trim()
            OptionalFieldLabel(
                text = shownLabel,
                isOptional = isOptional,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            CompositionLocalProvider(LocalFieldLabel provides shownLabel) {
                content()
            }
        }
    }
}
