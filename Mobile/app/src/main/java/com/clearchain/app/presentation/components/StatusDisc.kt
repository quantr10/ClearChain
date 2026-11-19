package com.clearchain.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.clearchain.app.ui.theme.Alpha
import com.clearchain.app.ui.theme.StatusDiscIconSize
import com.clearchain.app.ui.theme.StatusDiscSize
import com.clearchain.app.ui.theme.StatusRingsSize

/**
 * The design system's status picture: [icon] on a `primary-container` disc inside two rings of
 * `primary`. Empty states, Verify email and the onboarding confirmation use it. It's decorative,
 * so it has no content description.
 */
@Composable
fun StatusDisc(icon: ImageVector, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(StatusRingsSize)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = primary.copy(alpha = Alpha.RingOuter), radius = size.width / 2)
            drawCircle(color = primary.copy(alpha = Alpha.RingInner), radius = size.width / 2.8f)
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(StatusDiscSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(StatusDiscIconSize),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
