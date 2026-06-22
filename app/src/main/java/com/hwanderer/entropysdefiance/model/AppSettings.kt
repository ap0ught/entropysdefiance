// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

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
    val colorTheme: String = "Green", // Default to Green theme
    val leftHanded: Boolean = false, // Mirror controls (FAB, drawer swipe zone, calendar cut) to the left

    // Gesture settings
    val enableDoubleTapForDialer: Boolean = true,
    
    // Backup settings
    val enableAutomaticBackup: Boolean = false,
    val backupIntervalHours: Int = 24,
    
    // Task file sync settings
    val taskFilePath: String = "", // Empty means use default location (file path or URI depending on Android version)

    // Tutorial settings
    val tutorialCompleted: Boolean = false
) 