package com.example.entropysdefiance.ui.theme

import androidx.compose.ui.graphics.Color

// Define color schemes for different themes
// Each ColorScheme contains all the necessary colors for a complete theme

data class ColorPalette(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val displayName: String
)

// Green Theme (Default)
val GreenPalette = ColorPalette(
    primary = Color(0xFF487E4A),
    onPrimary = Color(0xFF003314),
    secondary = Color(0xFF589B5A),
    tertiary = Color(0xFF69B76B),
    displayName = "Green"
)

// Blue Theme
val BluePalette = ColorPalette(
    primary = Color(0xFF3F51B5),
    onPrimary = Color(0xFF000B5C),
    secondary = Color(0xFF5C6BC0),
    tertiary = Color(0xFF7986CB),
    displayName = "Blue"
)

// Purple Theme
val PurplePalette = ColorPalette(
    primary = Color(0xFF673AB7),
    onPrimary = Color(0xFF2A0463),
    secondary = Color(0xFF7E57C2),
    tertiary = Color(0xFF9575CD),
    displayName = "Purple"
)

// Orange Theme
val OrangePalette = ColorPalette(
    primary = Color(0xFFFF9800),
    onPrimary = Color(0xFF502F00),
    secondary = Color(0xFFFFA726),
    tertiary = Color(0xFFFFB74D),
    displayName = "Orange"
)

// Red Theme
val RedPalette = ColorPalette(
    primary = Color(0xFFF44336),
    onPrimary = Color(0xFF6E1210),
    secondary = Color(0xFFEF5350),
    tertiary = Color(0xFFE57373),
    displayName = "Red"
)

// Teal Theme
val TealPalette = ColorPalette(
    primary = Color(0xFF009688),
    onPrimary = Color(0xFF003D36),
    secondary = Color(0xFF26A69A),
    tertiary = Color(0xFF4DB6AC),
    displayName = "Teal"
)

// Pink Theme
val PinkPalette = ColorPalette(
    primary = Color(0xFFE91E63),
    onPrimary = Color(0xFF5F0A2B),
    secondary = Color(0xFFEC407A),
    tertiary = Color(0xFFF06292),
    displayName = "Pink"
)

// List of all available color palettes
val availableColorPalettes = listOf(
    GreenPalette,
    BluePalette,
    PurplePalette,
    OrangePalette,
    RedPalette,
    TealPalette,
    PinkPalette
)