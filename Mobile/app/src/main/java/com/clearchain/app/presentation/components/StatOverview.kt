package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** One figure on a [StatOverview]. */
data class StatTile(
    val icon: ImageVector,
    val label: String,
    val value: Int
)

/**
 * The headline figure in a large tile, with the supporting figures stacked beside it:
 * the admin's listings, a grocery's listings and an NGO's inventory all read this way.
 */
@Composable
fun StatOverview(
    hero: StatTile,
    secondary: List<StatTile>,
    modifier: Modifier = Modifier
) {
    Row(
        // Intrinsic height ties the hero tile to the ones stacked beside it.
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = hero.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = hero.value.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = hero.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            secondary.forEach { tile ->
                SecondaryStatTile(tile, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SecondaryStatTile(tile: StatTile, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tile.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = tile.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = tile.value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
