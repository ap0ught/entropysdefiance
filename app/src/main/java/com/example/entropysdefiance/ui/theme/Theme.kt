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

// New colors for FAB variants - lighter versions of primary
private val md_theme_dark_primaryVariant1 = Color(0xFF589B5A)  // 20% lighter than primary
private val md_theme_dark_primaryVariant2 = Color(0xFF69B76B)  // 40% lighter than primary

// Create dark color scheme with the provided color palette
private fun createDarkColorScheme(colorPalette: ColorPalette) = darkColorScheme(
    primary = colorPalette.primary,
    onPrimary = colorPalette.onPrimary,
    surface = Color.Transparent,  // Transparent for glass effect
    surfaceVariant = Color(0x11FFFFFF),  // Semi-transparent white for subtle contrast
    background = Color.Transparent,  // Transparent for glass effect
    onBackground = Color(0xFFE1E1E1),  // Soft white
    onSurface = Color(0xFFE1E1E1),
    outline = Color(0xFF858585),  // Subtle outline for cards
    surfaceTint = Color.Transparent,
    inverseSurface = Color.Transparent,
    surfaceBright = Color.Transparent,
    surfaceContainer = Color.Transparent,
    // Add the new variants from the palette
    secondary = colorPalette.secondary,
    tertiary = colorPalette.tertiary
)

@Composable
fun EntropysDefianceTheme(
    colorPalette: ColorPalette = GreenPalette,  // Use Green palette as default
    content: @Composable () -> Unit
) {
    val colorScheme = createDarkColorScheme(colorPalette)
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}