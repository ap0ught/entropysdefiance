package com.example.entropysdefiance.model

/**
 * Data class to store application settings
 */
data class AppSettings(
    // Calendar settings
    val showCalendarWidget: Boolean = true,
    val calendarDaysToShow: Int = 7,
    val selectedCalendarIds: Set<Long> = emptySet(),
    val calculationCalendarIds: Set<Long> = emptySet(), // Calendars used for free time calculation
    
    // Free time calculation settings
    val productivityRatio: Float = 0.5f, // How much of time is productive (0.0 to 1.0)
    
    // Appearance settings
    val useDarkTheme: Boolean = false,
    val colorTheme: String = "Green", // Default to Green theme
    
    // Gesture settings
    val enableDoubleTapForDialer: Boolean = true,
    
    // Backup settings
    val enableAutomaticBackup: Boolean = false,
    val backupIntervalHours: Int = 24
) 