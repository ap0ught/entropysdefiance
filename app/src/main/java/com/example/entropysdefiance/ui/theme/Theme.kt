package com.example.entropysdefiance.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern dark theme colors
private val md_theme_dark_primary = Color(0xFF487E4A)  // Material green
private val md_theme_dark_onPrimary = Color(0xFF003314)
private val md_theme_dark_background = Color.Transparent  // Changed from solid color to transparent
private val md_theme_dark_surface = Color.Transparent  // Changed from solid color to transparent
private val md_theme_dark_surfaceVariant = Color(0x11FFFFFF)  // Semi-transparent white for subtle contrast
private val md_theme_dark_onBackground = Color(0xFFE1E1E1)  // Soft white
private val md_theme_dark_onSurface = Color(0xFFE1E1E1)
private val md_theme_dark_outline = Color(0xFF858585)  // Subtle outline for cards

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    surface = md_theme_dark_surface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    onSurface = md_theme_dark_onSurface,
    outline = md_theme_dark_outline,
    surfaceTint = md_theme_dark_background,
    inverseSurface = md_theme_dark_background,
    surfaceBright = md_theme_dark_background,
    surfaceContainer = md_theme_dark_background
)

@Composable
fun EntropysDefianceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}