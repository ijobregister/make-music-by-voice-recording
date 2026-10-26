package com.example.voicesongstudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = StudioPrimary,
    onPrimary = TextPrimary,
    secondary = StudioAccentCyan,
    onSecondary = StudioDarkBg,
    tertiary = StudioAccentGold,
    background = StudioDarkBg,
    surface = StudioCardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = TextSecondary
)

@Composable
fun VoiceSongStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
