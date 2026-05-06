package com.hwanderer.entropysdefiance.ui.theme

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

// Indigo Theme
val IndigoPalette = ColorPalette(
    primary = Color(0xFF3F51B5),
    onPrimary = Color(0xFF000B5C),
    secondary = Color(0xFF5C6BC0),
    tertiary = Color(0xFF7986CB),
    displayName = "Indigo"
)

// Cyan Theme
val CyanPalette = ColorPalette(
    primary = Color(0xFF00BCD4),
    onPrimary = Color(0xFF00363A),
    secondary = Color(0xFF26C6DA),
    tertiary = Color(0xFF4DD0E1),
    displayName = "Cyan"
)

// Deep Purple Theme
val DeepPurplePalette = ColorPalette(
    primary = Color(0xFF673AB7),
    onPrimary = Color(0xFF2A0463),
    secondary = Color(0xFF7E57C2),
    tertiary = Color(0xFF9575CD),
    displayName = "Deep Purple"
)

// Light Blue Theme
val LightBluePalette = ColorPalette(
    primary = Color(0xFF03A9F4),
    onPrimary = Color(0xFF002A3A),
    secondary = Color(0xFF29B6F6),
    tertiary = Color(0xFF4FC3F7),
    displayName = "Light Blue"
)

// Lime Theme
val LimePalette = ColorPalette(
    primary = Color(0xFFCDDC39),
    onPrimary = Color(0xFF2A3A00),
    secondary = Color(0xFFD4E157),
    tertiary = Color(0xFFDCE775),
    displayName = "Lime"
)

// Amber Theme
val AmberPalette = ColorPalette(
    primary = Color(0xFFFFC107),
    onPrimary = Color(0xFF3A2A00),
    secondary = Color(0xFFFFCA28),
    tertiary = Color(0xFFFFD54F),
    displayName = "Amber"
)

// Deep Orange Theme
val DeepOrangePalette = ColorPalette(
    primary = Color(0xFFFF5722),
    onPrimary = Color(0xFF3A0A00),
    secondary = Color(0xFFFF7043),
    tertiary = Color(0xFFFF8A65),
    displayName = "Deep Orange"
)

// Brown Theme
val BrownPalette = ColorPalette(
    primary = Color(0xFF795548),
    onPrimary = Color(0xFF2A1A14),
    secondary = Color(0xFF8D6E63),
    tertiary = Color(0xFFA1887F),
    displayName = "Brown"
)

// Grey Theme
val GreyPalette = ColorPalette(
    primary = Color(0xFF607D8B),
    onPrimary = Color(0xFF1A2A2F),
    secondary = Color(0xFF78909C),
    tertiary = Color(0xFF90A4AE),
    displayName = "Grey"
)

// Blue Grey Theme
val BlueGreyPalette = ColorPalette(
    primary = Color(0xFF455A64),
    onPrimary = Color(0xFF0F1416),
    secondary = Color(0xFF546E7A),
    tertiary = Color(0xFF607D8B),
    displayName = "Blue Grey"
)

// List of all available color palettes
val availableColorPalettes = listOf(
    GreenPalette,
    BluePalette,
    PurplePalette,
    OrangePalette,
    RedPalette,
    TealPalette,
    PinkPalette,
    IndigoPalette,
    CyanPalette,
    DeepPurplePalette,
    LightBluePalette,
    LimePalette,
    AmberPalette,
    DeepOrangePalette,
    BrownPalette,
    GreyPalette,
    BlueGreyPalette
)