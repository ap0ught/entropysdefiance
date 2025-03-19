package com.example.entropysdefiance.model

/**
 * Data class to store application settings
 */
data class AppSettings(
    // Calendar settings
    val showCalendarWidget: Boolean = true,
    val calendarDaysToShow: Int = 7,
    
    // Appearance settings
    val useDarkTheme: Boolean = false,
    
    // Gesture settings
    val enableDoubleTapForDialer: Boolean = true
) 