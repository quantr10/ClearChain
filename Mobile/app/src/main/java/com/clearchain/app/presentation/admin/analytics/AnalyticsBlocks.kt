package com.clearchain.app.presentation.admin.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearchain.app.presentation.components.AvatarImage
import com.clearchain.app.presentation.components.DashboardSection
import java.util.Locale
import kotlin.math.roundToInt

// The pieces the Stats & Analytics cards are built from. Each one pairs a figure with the
// words that say what it counts, so no number on the screen stands without its label.

/** A section card that can be marked when the admin home linked straight to it. */
@Composable
internal fun AnalyticsCard(
    section: AnalyticsSection,
    title: String,
    highlighted: AnalyticsSection?,
    content: @Composable ColumnScope.() -> Unit
) {
    val isHighlighted = highlighted == section
    val borderColor by animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "sectionHighlight"
    )
    DashboardSection(
        title = title,
        modifier = Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp)),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
        }
    )
}

// ── Figures ──────────────────────────────────────────────────────────────────

@Composable
internal fun SubHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
internal fun EmptyChartNote(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp)
    )
}

/**
 * A rounded track filled to [fraction] in [color]. Silent to accessibility services: the
 * figure beside it is what they read out. A NaN fraction (0 / 0) draws an empty track.
 */
@Composable
internal fun MeterBar(fraction: Float, color: Color, height: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(height / 2)
    // coerceIn passes NaN through, and fillMaxWidth(NaN) throws when it rounds the width.
    val filled = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {}
    ) {
        Box(
            Modifier
                .fillMaxWidth(filled)
                .fillMaxHeight()
                .clip(shape)
                .background(color)
        )
    }
}

// ── Leaderboard ──────────────────────────────────────────────────────────────

/**
 * One place on a leaderboard: rank, who, the score, and a bar showing it against
 * [fraction] of the scale. [caption], when there is one, sits under the name. Tapping calls
 * [onClick]; without one the row isn't clickable.
 */
@Composable
internal fun LeaderRow(
    rank: Int,
    name: String,
    pictureUrl: String?,
    value: String,
    fraction: Float,
    color: Color,
    caption: String?,
    onClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LeaderGap)
        ) {
            RankBadge(rank)
            AvatarImage(imageUrl = pictureUrl, name = name, size = LEADER_AVATAR_SIZE)
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (caption != null) {
                    Text(
                        caption,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        // Starts under the name, so the bars of every row share one baseline.
        MeterBar(
            fraction = fraction,
            color = color,
            height = 4.dp,
            modifier = Modifier
                .padding(start = LeaderBadge + LeaderGap + LEADER_AVATAR_SIZE.dp + LeaderGap)
                .fillMaxWidth()
        )
    }
}

/** The podium gets the brand colour; everyone below it a quiet badge. */
@Composable
private fun RankBadge(rank: Int) {
    val podium = rank <= 3
    Surface(
        shape = CircleShape,
        color = if (podium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(LeaderBadge)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                rank.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (podium) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val LeaderBadge = 24.dp
private val LeaderGap = 10.dp
private const val LEADER_AVATAR_SIZE = 32

// ── Formatting ───────────────────────────────────────────────────────────────

internal fun percentOf(rate: Double): Int = (rate * 100).roundToInt()

internal fun formatRating(value: Double): String = String.format(Locale.getDefault(), "%.1f", value)

internal fun formatDuration(hours: Double?): String = when {
    hours == null -> "–"
    hours < 1 -> "${(hours * 60).roundToInt()}m"
    hours < 48 -> "${hours.roundToInt()}h"
    else -> "${(hours / 24).roundToInt()}d"
}
