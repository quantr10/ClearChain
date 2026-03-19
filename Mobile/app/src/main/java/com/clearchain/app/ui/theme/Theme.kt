// ═══════════════════════════════════════════════════════════════════════════════
// Theme.kt — Updated with semantic colors and proper dark theme
// ═══════════════════════════════════════════════════════════════════════════════

package com.clearchain.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary               = LightPrimary,
    onPrimary             = LightOnPrimary,
    primaryContainer      = LightPrimaryContainer,
    onPrimaryContainer    = LightOnPrimaryContainer,
    secondary             = LightSecondary,
    onSecondary           = Color.White,
    secondaryContainer    = Color(0xFFDBEAFE),
    onSecondaryContainer  = Color(0xFF1E40AF),
    tertiary              = LightTertiary,
    onTertiary            = Color.White,
    tertiaryContainer     = Color(0xFFEDE9FE),
    onTertiaryContainer   = Color(0xFF5B21B6),
    background            = LightBackground,
    onBackground          = LightOnBackground,
    surface               = LightSurface,
    onSurface             = LightOnSurface,
    surfaceVariant        = LightSurfaceVariant,
    onSurfaceVariant      = LightOnSurfaceVariant,
    error                 = LightError,
    errorContainer        = LightErrorContainer,
    onErrorContainer      = LightOnErrorContainer,
    outline               = Gray300,
    outlineVariant        = Gray200,
)

private val DarkColorScheme = darkColorScheme(
    primary               = DarkPrimary,
    onPrimary             = DarkOnPrimary,
    primaryContainer      = DarkPrimaryContainer,
    onPrimaryContainer    = DarkOnPrimaryContainer,
    secondary             = DarkSecondary,
    onSecondary           = Color(0xFF003373),
    secondaryContainer    = Color(0xFF1E3A5F),
    onSecondaryContainer  = Color(0xFFBFDBFE),
    tertiary              = DarkTertiary,
    onTertiary            = Color(0xFF2D1065),
    tertiaryContainer     = Color(0xFF3B1A6E),
    onTertiaryContainer   = Color(0xFFDDD6FE),
    background            = DarkBackground,
    onBackground          = DarkOnBackground,
    surface               = DarkSurface,
    onSurface             = DarkOnSurface,
    surfaceVariant        = DarkSurfaceVariant,
    onSurfaceVariant      = DarkOnSurfaceVariant,
    error                 = DarkError,
    errorContainer        = DarkErrorContainer,
    onErrorContainer      = DarkOnErrorContainer,
    outline               = Color(0xFF475569),
    outlineVariant        = Color(0xFF334155),
)

@Composable
fun ClearChainTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}