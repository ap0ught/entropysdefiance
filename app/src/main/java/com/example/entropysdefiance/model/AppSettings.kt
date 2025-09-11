package com.example.entropysdefiance.model

/**
 * Data class to store application settings
 */
data class AppSettings(
    // Calendar settings
    val showCalendarWidget: Boolean = true,
    val calendarDaysToShow: Int = 7,
    val selectedCalendarIds: Set<Long> = emptySet(),
    
    // Appearance settings
    val useDarkTheme: Boolean = false,
    val colorTheme: String = "Green", // Default to Green theme
    
    // Gesture settings
    val enableDoubleTapForDialer: Boolean = true,
    
    // Backup settings
    val enableAutomaticBackup: Boolean = false,
    val backupIntervalHours: Int = 24
) 