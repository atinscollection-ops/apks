package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DustyPinkPrimary,
    onPrimary = DustyPinkOnPrimary,
    primaryContainer = DustyPinkPrimaryContainer,
    onPrimaryContainer = DustyPinkOnPrimaryContainer,
    secondary = DustyPinkSecondary,
    onSecondary = DustyPinkOnSecondary,
    secondaryContainer = DustyPinkSecondaryContainer,
    onSecondaryContainer = DustyPinkOnSecondaryContainer,
    background = DustyPinkBackground,
    onBackground = DustyPinkOnBackground,
    surface = DustyPinkSurface,
    onSurface = DustyPinkOnSurface,
    surfaceVariant = DustyPinkSurfaceVariant,
    onSurfaceVariant = DustyPinkOnSurfaceVariant,
    outline = DustyPinkOutline,
    outlineVariant = DustyPinkOutlineVariant,
    error = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = DustyPinkPrimary,
    onPrimary = DustyPinkOnPrimary,
    primaryContainer = DustyPinkOnPrimaryContainer,
    onPrimaryContainer = DustyPinkPrimaryContainer,
    secondary = DustyPinkSecondary,
    onSecondary = DustyPinkOnSecondary,
    background = DustyPinkOnBackground,
    onBackground = DustyPinkBackground,
    surface = DustyPinkOnBackground,
    onSurface = DustyPinkBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
