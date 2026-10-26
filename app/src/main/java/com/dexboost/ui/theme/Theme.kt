package com.dexboost.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = ElectricGreen,
    onPrimary = Color(0xFF052E16),
    secondary = CyanAccent,
    onSecondary = Color(0xFF083344),
    background = CharcoalBackground,
    onBackground = CharcoalOnBackground,
    surface = CharcoalSurface,
    onSurface = CharcoalOnSurface,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = Color(0xFFC9C9CF),
    outline = CharcoalOutline,
    error = ErrorRed
)

private val LightScheme = lightColorScheme(
    primary = ElectricGreenDark,
    onPrimary = Color.White,
    secondary = CyanAccentDark,
    onSecondary = Color.White,
    background = Color(0xFFF6F6F7),
    onBackground = Color(0xFF121214),
    surface = Color.White,
    onSurface = Color(0xFF121214),
    surfaceVariant = Color(0xFFEDEDEF),
    onSurfaceVariant = Color(0xFF3A3A40),
    outline = Color(0xFFD4D4D8),
    error = Color(0xFFB91C1C)
)

@Composable
fun DexBoostTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = DexBoostTypography,
        content = content
    )
}
