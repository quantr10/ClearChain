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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    action: (@Composable () -> Unit)? = null,
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
        action = action,
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

// ── Leaderboard ──────────────────────────────────────────────────────────────

/**
 * One place on a leaderboard: rank, who, the score, and a bar showing it against
 * [fraction] of the scale. Tapping opens the organization's profile.
 */
@Composable
internal fun LeaderRow(
    rank: Int,
    name: String,
    pictureUrl: String?,
    value: String,
    fraction: Float,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LeaderGap)
        ) {
            RankBadge(rank)
            AvatarImage(imageUrl = pictureUrl, name = name, size = LEADER_AVATAR_SIZE)
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        // Starts under the name, so the bars of every row share one baseline.
        Box(
            modifier = Modifier
                .padding(start = LeaderBadge + LeaderGap + LEADER_AVATAR_SIZE.dp + LeaderGap)
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
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
