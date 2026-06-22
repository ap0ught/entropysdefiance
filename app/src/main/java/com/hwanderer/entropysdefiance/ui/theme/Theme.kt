// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

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

/** Sentinel [AppSettings.colorTheme] value meaning "follow the system / Material You accent". */
const val SYSTEM_COLOR_THEME = "System"

/**
 * Build a full [ColorPalette] from a single seed (accent) color, deriving lighter secondary and
 * tertiary variants so the FAB gradient stays consistent, and a contrasting onPrimary.
 */
fun paletteFromSeed(seed: Color, name: String): ColorPalette {
    fun lighten(c: Color, fraction: Float) = Color(
        red = c.red + (1f - c.red) * fraction,
        green = c.green + (1f - c.green) * fraction,
        blue = c.blue + (1f - c.blue) * fraction,
        alpha = 1f
    )
    return ColorPalette(
        primary = seed,
        onPrimary = if (seed.luminance() > 0.5f) Color(0xFF1A1A1A) else Color.White,
        secondary = lighten(seed, 0.20f),
        tertiary = lighten(seed, 0.40f),
        displayName = name
    )
}

/** Parse a "#RRGGBB" (or "#AARRGGBB") string to a [Color], or null if invalid. */
fun parseHexColor(hex: String): Color? = try {
    Color(android.graphics.Color.parseColor(hex.trim()))
} catch (e: IllegalArgumentException) {
    null
}

/**
 * Resolve a stored [AppSettings.colorTheme] string into a [ColorPalette]:
 *  - [SYSTEM_COLOR_THEME] → Material You accent derived from the wallpaper (Android 12+),
 *  - a "#RRGGBB" string → a custom palette seeded from that color,
 *  - otherwise a named preset (falling back to Green).
 */
fun resolveColorPalette(theme: String, context: Context): ColorPalette = when {
    theme == SYSTEM_COLOR_THEME ->
        paletteFromSeed(dynamicDarkColorScheme(context).primary, SYSTEM_COLOR_THEME)
    theme.startsWith("#") ->
        parseHexColor(theme)?.let { paletteFromSeed(it, theme) } ?: GreenPalette
    else ->
        availableColorPalettes.find { it.displayName == theme } ?: GreenPalette
}