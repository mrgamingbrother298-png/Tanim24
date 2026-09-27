package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NovaColorScheme = darkColorScheme(
    primary = NovaNeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),

    secondary = NovaNeonPurple,
    onSecondary = Color(0xFF380062),
    secondaryContainer = Color(0xFF55198B),
    onSecondaryContainer = Color(0xFFEEDBFF),

    tertiary = NovaNeonEmerald,
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF005234),
    onTertiaryContainer = Color(0xFF6FF7B5),

    background = NovaDarkBackground,
    onBackground = NovaTextPrimary,
    surface = NovaSurfaceDark,
    onSurface = NovaTextPrimary,
    surfaceVariant = NovaSurfaceCard,
    onSurfaceVariant = NovaTextSecondary,
    outline = Color(0xFF334155),
    error = NovaNeonRed,
    onError = Color.White
)

@Composable
fun NovaAiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NovaColorScheme,
        typography = Typography,
        content = content
    )
}
