package com.clearchain.app.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clearchain.app.ui.theme.CardShape

@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        )
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
fun ShimmerCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(140.dp), shape = RoundedCornerShape(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(18.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.width(70.dp).height(24.dp), shape = RoundedCornerShape(50))
                ShimmerBox(modifier = Modifier.width(50.dp).height(24.dp), shape = RoundedCornerShape(50))
            }
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(1.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(modifier = Modifier.width(90.dp).height(12.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp))
            }
        }
    }
}

@Composable
fun ShimmerStatCardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            ShimmerBox(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBox(modifier = Modifier.width(60.dp).height(22.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp))
            }
        }
    }
}

@Composable
fun ShimmerListRow(
    lines: Int = 2,
    imageWidth: Dp = 56.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerBox(modifier = Modifier.size(imageWidth), shape = RoundedCornerShape(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            repeat(lines) { index ->
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(if (index == 0) 1f else 0.6f)
                        .height(if (index == 0) 16.dp else 12.dp)
                )
            }
        }
    }
}
