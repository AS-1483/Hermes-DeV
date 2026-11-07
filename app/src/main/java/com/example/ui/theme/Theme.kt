package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = TerminalBlack,
    primaryContainer = CyanDark.copy(alpha = 0.2f),
    onPrimaryContainer = CyanPrimary,
    secondary = AmberAccent,
    onSecondary = TerminalBlack,
    secondaryContainer = AmberAccent.copy(alpha = 0.2f),
    onSecondaryContainer = AmberAccent,
    tertiary = EmeraldSuccess,
    background = CyberDarkBg,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCard,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardStroke,
    error = RoseError,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // Hermes Dev is fundamentally a sleek developer IDE / dark-themed terminal aesthetic

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
