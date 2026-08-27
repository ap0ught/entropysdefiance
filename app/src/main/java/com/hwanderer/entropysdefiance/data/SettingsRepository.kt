// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hwanderer.entropysdefiance.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val TAG = "SettingsRepository"

/**
 * Extension property to create a DataStore instance once for the application lifecycle
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

/**
 * Repository for managing app settings persistence
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    // Secondary constructor for backward compatibility
    constructor(context: Context) : this(context.settingsDataStore)

    // Define the keys for preferences
    private object PreferencesKeys {
        val SHOW_CALENDAR_WIDGET = booleanPreferencesKey("show_calendar_widget")
        val CALENDAR_DAYS_TO_SHOW = intPreferencesKey("calendar_days_to_show")
        val ENABLE_DOUBLE_TAP_DIALER = booleanPreferencesKey("enable_double_tap_dialer")
        val ENABLE_DND_IN_ZEN_MODE = booleanPreferencesKey("enable_dnd_in_zen_mode")
        val DND_TOTAL_SILENCE = booleanPreferencesKey("dnd_total_silence")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val LEFT_HANDED = booleanPreferencesKey("left_handed")
        val ENABLE_AUTOMATIC_BACKUP = booleanPreferencesKey("enable_automatic_backup")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        
        // Use a string set to store the selected calendar IDs
        val SELECTED_CALENDAR_IDS = stringSetPreferencesKey("selected_calendar_ids")
        val CALCULATION_CALENDAR_IDS = stringSetPreferencesKey("calculation_calendar_ids")
        
        // Free time calculation settings
        val PRODUCTIVITY_RATIO = floatPreferencesKey("productivity_ratio")
        
        // Task file sync settings
        val TASK_FILE_PATH = stringPreferencesKey("task_file_path")

        // Tutorial settings
        val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed")
    }

    /**
     * Flow of AppSettings from DataStore
     * This will emit a new value whenever the settings change
     */
    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            // If an error occurs during reading settings
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences: ${exception.message}", exception)
                emit(emptyPreferences())
            } else {
                Log.e(TAG, "Critical error during preferences loading: ${exception.message}", exception)
                throw exception
            }
        }
        .map { preferences ->
            // Map preferences to AppSettings
            val showCalendarWidget = preferences[PreferencesKeys.SHOW_CALENDAR_WIDGET] ?: true
            val calendarDaysToShow = preferences[PreferencesKeys.CALENDAR_DAYS_TO_SHOW] ?: 7
            val enableDoubleTapForDialer = preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER] ?: true
            val colorTheme = preferences[PreferencesKeys.COLOR_THEME] ?: "System"
            val leftHanded = preferences[PreferencesKeys.LEFT_HANDED] ?: false
            val enableDndInZenMode = preferences[PreferencesKeys.ENABLE_DND_IN_ZEN_MODE] ?: false
            val dndTotalSilence = preferences[PreferencesKeys.DND_TOTAL_SILENCE] ?: false
            val enableAutomaticBackup = preferences[PreferencesKeys.ENABLE_AUTOMATIC_BACKUP] ?: false
            val backupIntervalHours = preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] ?: 24
            val productivityRatio = preferences[PreferencesKeys.PRODUCTIVITY_RATIO] ?: 0.5f
            val taskFilePath = preferences[PreferencesKeys.TASK_FILE_PATH] ?: ""
            val tutorialCompleted = preferences[PreferencesKeys.TUTORIAL_COMPLETED] ?: false

            // Extract selected calendar IDs from the string set
            val selectedCalendarIdStrings = preferences[PreferencesKeys.SELECTED_CALENDAR_IDS] ?: emptySet()
            val selectedCalendarIds = selectedCalendarIdStrings
                .mapNotNull { it.toLongOrNull() }
                .toSet()
            
            // Extract calculation calendar IDs from the string set
            val calculationCalendarIdStrings = preferences[PreferencesKeys.CALCULATION_CALENDAR_IDS] ?: emptySet()
            val calculationCalendarIds = calculationCalendarIdStrings
                .mapNotNull { it.toLongOrNull() }
                .toSet()

            AppSettings(
                showCalendarWidget = showCalendarWidget,
                calendarDaysToShow = calendarDaysToShow,
                enableDoubleTapForDialer = enableDoubleTapForDialer,
                colorTheme = colorTheme,
                leftHanded = leftHanded,
                enableDndInZenMode = enableDndInZenMode,
                dndTotalSilence = dndTotalSilence,
                selectedCalendarIds = selectedCalendarIds,
                calculationCalendarIds = calculationCalendarIds,
                productivityRatio = productivityRatio,
                enableAutomaticBackup = enableAutomaticBackup,
                backupIntervalHours = backupIntervalHours,
                taskFilePath = taskFilePath,
                tutorialCompleted = tutorialCompleted
            )
        }

    /**
     * Update show calendar widget setting
     */
    suspend fun updateShowCalendarWidget(show: Boolean) {
        Log.d(TAG, "Saving showCalendarWidget: $show")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_CALENDAR_WIDGET] = show
            }
            Log.d(TAG, "Successfully saved showCalendarWidget")        } catch (e: Exception) {
            Log.e(TAG, "Error saving showCalendarWidget: ${e.message}", e)
        }
    }

    /**
     * Update calendar days to show setting
     */
    suspend fun updateCalendarDaysToShow(days: Int) {
        Log.d(TAG, "Saving calendarDaysToShow: $days")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CALENDAR_DAYS_TO_SHOW] = days
            }
            Log.d(TAG, "Successfully saved calendarDaysToShow")        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendarDaysToShow: ${e.message}", e)
        }
    }

    /**
     * Update double tap for dialer setting
     */
    suspend fun updateEnableDoubleTapDialer(enable: Boolean) {
        Log.d(TAG, "Saving enableDoubleTapDialer: $enable")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER] = enable
            }
            Log.d(TAG, "Successfully saved enableDoubleTapDialer")        } catch (e: Exception) {
            Log.e(TAG, "Error saving enableDoubleTapDialer: ${e.message}", e)
        }
    }
    
    /**
     * Update color theme setting
     */
    suspend fun updateColorTheme(theme: String) {
        Log.d(TAG, "Saving colorTheme: $theme")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.COLOR_THEME] = theme
            }
            Log.d(TAG, "Successfully saved colorTheme")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving colorTheme: ${e.message}", e)
        }
    }

    /**
     * Update left-handed (mirrored layout) setting
     */
    suspend fun updateLeftHanded(leftHanded: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.LEFT_HANDED] = leftHanded
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving leftHanded: ${e.message}", e)
        }
    }

    /**
     * Update whether Zen Mode switches on Do Not Disturb
     */
    suspend fun updateEnableDndInZenMode(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLE_DND_IN_ZEN_MODE] = enabled
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving enableDndInZenMode: ${e.message}", e)
        }
    }

    /**
     * Update whether Zen Mode's Do Not Disturb uses total silence rather than priority only
     */
    suspend fun updateDndTotalSilence(totalSilence: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.DND_TOTAL_SILENCE] = totalSilence
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving dndTotalSilence: ${e.message}", e)
        }
    }

    /**
     * Update selected calendar IDs
     */
    suspend fun updateSelectedCalendarIds(calendarIds: Set<Long>) {
        Log.d(TAG, "Saving selectedCalendarIds: $calendarIds")
        try {
            // Convert Long IDs to strings for storage
            val calendarIdStrings = calendarIds.map { it.toString() }.toSet()
            
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_CALENDAR_IDS] = calendarIdStrings
            }
            Log.d(TAG, "Successfully saved selectedCalendarIds")        } catch (e: Exception) {
            Log.e(TAG, "Error saving selectedCalendarIds: ${e.message}", e)
        }
    }
    
    /**
     * Update calculation calendar IDs (for free time calculation)
     */
    suspend fun updateCalculationCalendarIds(calendarIds: Set<Long>) {
        Log.d(TAG, "Saving calculationCalendarIds: $calendarIds")
        try {
            // Convert Long IDs to strings for storage
            val calendarIdStrings = calendarIds.map { it.toString() }.toSet()
            
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.CALCULATION_CALENDAR_IDS] = calendarIdStrings
            }
            Log.d(TAG, "Successfully saved calculationCalendarIds")        } catch (e: Exception) {
            Log.e(TAG, "Error saving calculationCalendarIds: ${e.message}", e)
        }
    }
    
    /**
     * Update productivity ratio (for free time calculation)
     */
    suspend fun updateProductivityRatio(ratio: Float) {
        Log.d(TAG, "Saving productivityRatio: $ratio")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.PRODUCTIVITY_RATIO] = ratio.coerceIn(0f, 1f)
            }
            Log.d(TAG, "Successfully saved productivityRatio")        } catch (e: Exception) {
            Log.e(TAG, "Error saving productivityRatio: ${e.message}", e)
        }
    }
    
    /**
     * Update all settings at once
     */
    suspend fun updateAllSettings(settings: AppSettings) {
        Log.d(TAG, "Saving all settings: $settings")
        try {
            // Convert Long IDs to strings for storage
            val calendarIdStrings = settings.selectedCalendarIds.map { it.toString() }.toSet()
            val calculationCalendarIdStrings = settings.calculationCalendarIds.map { it.toString() }.toSet()
            
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_CALENDAR_WIDGET] = settings.showCalendarWidget
                preferences[PreferencesKeys.CALENDAR_DAYS_TO_SHOW] = settings.calendarDaysToShow
                preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER] = settings.enableDoubleTapForDialer
                preferences[PreferencesKeys.ENABLE_DND_IN_ZEN_MODE] = settings.enableDndInZenMode
                preferences[PreferencesKeys.DND_TOTAL_SILENCE] = settings.dndTotalSilence
                preferences[PreferencesKeys.COLOR_THEME] = settings.colorTheme
                preferences[PreferencesKeys.LEFT_HANDED] = settings.leftHanded
                preferences[PreferencesKeys.ENABLE_AUTOMATIC_BACKUP] = settings.enableAutomaticBackup
                preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] = settings.backupIntervalHours
                preferences[PreferencesKeys.SELECTED_CALENDAR_IDS] = calendarIdStrings
                preferences[PreferencesKeys.CALCULATION_CALENDAR_IDS] = calculationCalendarIdStrings
                preferences[PreferencesKeys.PRODUCTIVITY_RATIO] = settings.productivityRatio
                preferences[PreferencesKeys.TASK_FILE_PATH] = settings.taskFilePath
                preferences[PreferencesKeys.TUTORIAL_COMPLETED] = settings.tutorialCompleted
            }
            Log.d(TAG, "Successfully saved all settings")        } catch (e: Exception) {
            Log.e(TAG, "Error saving all settings: ${e.message}", e)
        }
    }
    
    /**
     * Update automatic backup setting
     */
    suspend fun updateAutomaticBackup(enabled: Boolean) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.ENABLE_AUTOMATIC_BACKUP] = enabled
            }
            Log.d(TAG, "Updated automatic backup setting: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating automatic backup setting: ${e.message}", e)
        }
    }
    
    /**
     * Update backup interval setting
     */
    suspend fun updateBackupInterval(hours: Int) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] = hours
            }
            Log.d(TAG, "Updated backup interval setting: $hours hours")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating backup interval setting: ${e.message}", e)
        }
    }
    
    /**
     * Update task file path setting
     */
    suspend fun updateTaskFilePath(path: String) {
        Log.d(TAG, "Saving taskFilePath: $path")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.TASK_FILE_PATH] = path
            }
            Log.d(TAG, "Successfully saved taskFilePath")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving taskFilePath: ${e.message}", e)
        }
    }

    /**
     * Update tutorial completed setting
     */
    suspend fun updateTutorialCompleted(completed: Boolean) {
        Log.d(TAG, "Saving tutorialCompleted: $completed")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.TUTORIAL_COMPLETED] = completed
            }
            Log.d(TAG, "Successfully saved tutorialCompleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving tutorialCompleted: ${e.message}", e)
        }
    }
    
    /**
     * Debug method to dump DataStore contents to logs
     */
    suspend fun dumpDataStoreContents() {
        try {
            val preferences = dataStore.data.first()
            Log.d(TAG, "DataStore Contents:")
            Log.d(TAG, "----------------")
            
            // Check for our specific keys
            val showCalendar = preferences[PreferencesKeys.SHOW_CALENDAR_WIDGET]
            val days = preferences[PreferencesKeys.CALENDAR_DAYS_TO_SHOW]
            val doubleTap = preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER]
            val colorTheme = preferences[PreferencesKeys.COLOR_THEME]
            val calendarIdStrings = preferences[PreferencesKeys.SELECTED_CALENDAR_IDS]
            
            Log.d(TAG, "SHOW_CALENDAR_WIDGET: $showCalendar")
            Log.d(TAG, "CALENDAR_DAYS_TO_SHOW: $days")
            Log.d(TAG, "ENABLE_DOUBLE_TAP_DIALER: $doubleTap")
            Log.d(TAG, "COLOR_THEME: $colorTheme")
            Log.d(TAG, "SELECTED_CALENDAR_IDS: $calendarIdStrings")
            
            // Dump all keys in the preferences
            Log.d(TAG, "All Keys:")
            preferences.asMap().forEach { (key, value) ->
                Log.d(TAG, "${key.name}: $value")
            }
            
            Log.d(TAG, "----------------")
        } catch (e: Exception) {
            Log.e(TAG, "Error dumping DataStore contents: ${e.message}", e)
        }
    }
} 