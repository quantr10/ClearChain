package com.clearchain.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal

/**
 * Shared gradient header used by both LoginScreen and RegisterScreen.
 * [navigationIcon] is placed in the top-start corner of the header (e.g. a back button).
 */
@Composable
fun AuthHeader(
    subtitle: String,
    navigationIcon: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.verticalGradient(colors = listOf(BrandTeal, BrandGreen))
            )
    ) {
        // Optional nav icon (back button for register)
        navigationIcon?.let {
            Box(modifier = Modifier.align(Alignment.TopStart)) { it() }
        }

        // Logo + app name + subtitle
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector   = Icons.Default.Eco,
                    contentDescription = null,
                    modifier      = Modifier.size(38.dp),
                    tint          = Color.White
                )
            }
            Text(
                text       = "ClearChain",
                fontSize   = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

/** Shared "or" divider between primary and secondary auth actions. */
@Composable
fun AuthDivider() {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text     = "or",
            modifier = Modifier.padding(horizontal = 12.dp),
            style    = MaterialTheme.typography.bodySmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
