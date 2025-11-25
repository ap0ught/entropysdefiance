package com.example.entropysdefiance.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.entropysdefiance.model.AppSettings
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
        val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
        val ENABLE_DOUBLE_TAP_DIALER = booleanPreferencesKey("enable_double_tap_dialer")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val ENABLE_AUTOMATIC_BACKUP = booleanPreferencesKey("enable_automatic_backup")
        val BACKUP_INTERVAL_HOURS = intPreferencesKey("backup_interval_hours")
        
        // Use a string set to store the selected calendar IDs
        val SELECTED_CALENDAR_IDS = stringSetPreferencesKey("selected_calendar_ids")
        val CALCULATION_CALENDAR_IDS = stringSetPreferencesKey("calculation_calendar_ids")
        
        // Free time calculation settings
        val PRODUCTIVITY_RATIO = floatPreferencesKey("productivity_ratio")
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
            val useDarkTheme = preferences[PreferencesKeys.USE_DARK_THEME] ?: false
            val enableDoubleTapForDialer = preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER] ?: true
            val colorTheme = preferences[PreferencesKeys.COLOR_THEME] ?: "Green"
            val enableAutomaticBackup = preferences[PreferencesKeys.ENABLE_AUTOMATIC_BACKUP] ?: false
            val backupIntervalHours = preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] ?: 24
            val productivityRatio = preferences[PreferencesKeys.PRODUCTIVITY_RATIO] ?: 0.5f
            
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
            
            Log.d(TAG, "Loading settings: showCalendar=$showCalendarWidget, days=$calendarDaysToShow, " +
                       "darkTheme=$useDarkTheme, doubleTap=$enableDoubleTapForDialer, " +
                       "colorTheme=$colorTheme, backup=$enableAutomaticBackup, " +
                       "selectedCalendars=${selectedCalendarIds.size}, " +
                       "calculationCalendars=${calculationCalendarIds.size}, " +
                       "productivityRatio=$productivityRatio")
            
            AppSettings(
                showCalendarWidget = showCalendarWidget,
                calendarDaysToShow = calendarDaysToShow,
                useDarkTheme = useDarkTheme,
                enableDoubleTapForDialer = enableDoubleTapForDialer,
                colorTheme = colorTheme,
                selectedCalendarIds = selectedCalendarIds,
                calculationCalendarIds = calculationCalendarIds,
                productivityRatio = productivityRatio,
                enableAutomaticBackup = enableAutomaticBackup,
                backupIntervalHours = backupIntervalHours
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
            Log.d(TAG, "Successfully saved showCalendarWidget")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
            Log.d(TAG, "Successfully saved calendarDaysToShow")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
            Log.e(TAG, "Error saving calendarDaysToShow: ${e.message}", e)
        }
    }

    /**
     * Update dark theme setting
     */
    suspend fun updateUseDarkTheme(useDarkTheme: Boolean) {
        Log.d(TAG, "Saving useDarkTheme: $useDarkTheme")
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.USE_DARK_THEME] = useDarkTheme
            }
            Log.d(TAG, "Successfully saved useDarkTheme")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
            Log.e(TAG, "Error saving useDarkTheme: ${e.message}", e)
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
            Log.d(TAG, "Successfully saved enableDoubleTapDialer")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
            Log.e(TAG, "Error saving colorTheme: ${e.message}", e)
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
            Log.d(TAG, "Successfully saved selectedCalendarIds")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
            Log.d(TAG, "Successfully saved calculationCalendarIds")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
            Log.d(TAG, "Successfully saved productivityRatio")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
                preferences[PreferencesKeys.USE_DARK_THEME] = settings.useDarkTheme
                preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER] = settings.enableDoubleTapForDialer
                preferences[PreferencesKeys.COLOR_THEME] = settings.colorTheme
                preferences[PreferencesKeys.ENABLE_AUTOMATIC_BACKUP] = settings.enableAutomaticBackup
                preferences[PreferencesKeys.BACKUP_INTERVAL_HOURS] = settings.backupIntervalHours
                preferences[PreferencesKeys.SELECTED_CALENDAR_IDS] = calendarIdStrings
                preferences[PreferencesKeys.CALCULATION_CALENDAR_IDS] = calculationCalendarIdStrings
                preferences[PreferencesKeys.PRODUCTIVITY_RATIO] = settings.productivityRatio
            }
            Log.d(TAG, "Successfully saved all settings")
            dumpDataStoreContents() // Debug - dump contents after updating
        } catch (e: Exception) {
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
            val darkTheme = preferences[PreferencesKeys.USE_DARK_THEME]
            val doubleTap = preferences[PreferencesKeys.ENABLE_DOUBLE_TAP_DIALER]
            val colorTheme = preferences[PreferencesKeys.COLOR_THEME]
            val calendarIdStrings = preferences[PreferencesKeys.SELECTED_CALENDAR_IDS]
            
            Log.d(TAG, "SHOW_CALENDAR_WIDGET: $showCalendar")
            Log.d(TAG, "CALENDAR_DAYS_TO_SHOW: $days")
            Log.d(TAG, "USE_DARK_THEME: $darkTheme")
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