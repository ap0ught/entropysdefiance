// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.EntropysDefianceApplication
import com.hwanderer.entropysdefiance.data.SettingsRepository
import com.hwanderer.entropysdefiance.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

/**
 * ViewModel for managing application settings
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    // Initialize the repository from the application instance
    private val repository = (application as EntropysDefianceApplication).settingsRepository
    
    // State for settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    // State for settings dialog visibility
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()
    
    // State for calendar selection visibility within settings
    private val _showCalendarSelection = MutableStateFlow(false)
    val showCalendarSelection: StateFlow<Boolean> = _showCalendarSelection.asStateFlow()
    
    // State for color palette selection visibility
    private val _showColorPaletteSelection = MutableStateFlow(false)
    val showColorPaletteSelection: StateFlow<Boolean> = _showColorPaletteSelection.asStateFlow()
    
    // State for statistics screen visibility
    private val _showStatisticsScreen = MutableStateFlow(false)
    val showStatisticsScreen: StateFlow<Boolean> = _showStatisticsScreen.asStateFlow()
    
    // State for individual settings screens
    private val _showCalendarSettings = MutableStateFlow(false)
    val showCalendarSettings: StateFlow<Boolean> = _showCalendarSettings.asStateFlow()
    
    private val _showAppearanceSettings = MutableStateFlow(false)
    val showAppearanceSettings: StateFlow<Boolean> = _showAppearanceSettings.asStateFlow()
    
    private val _showGesturesSettings = MutableStateFlow(false)
    val showGesturesSettings: StateFlow<Boolean> = _showGesturesSettings.asStateFlow()
    
    private val _showBackupSettings = MutableStateFlow(false)
    val showBackupSettings: StateFlow<Boolean> = _showBackupSettings.asStateFlow()
    
    private val _showFreeTimeSettings = MutableStateFlow(false)
    val showFreeTimeSettings: StateFlow<Boolean> = _showFreeTimeSettings.asStateFlow()
    
    private val _showFileSyncSettings = MutableStateFlow(false)
    val showFileSyncSettings: StateFlow<Boolean> = _showFileSyncSettings.asStateFlow()

    private val _showPermissionsSettings = MutableStateFlow(false)
    val showPermissionsSettings: StateFlow<Boolean> = _showPermissionsSettings.asStateFlow()

    private val _showCalculationCalendarSelection = MutableStateFlow(false)
    val showCalculationCalendarSelection: StateFlow<Boolean> = _showCalculationCalendarSelection.asStateFlow()
    
    init {
        Log.d(TAG, "Initializing SettingsViewModel")
        loadSettings()
    }
    
    /**
     * Load settings from persistent storage
     */
    private fun loadSettings() {
        Log.d(TAG, "Loading settings from repository")
        viewModelScope.launch {
            // Start collecting the settings flow
            repository.settingsFlow.collect { appSettings ->
                Log.d(TAG, "Received settings update: $appSettings")
                _settings.value = appSettings
            }
        }
    }
    
    /**
     * Save settings to persistent storage
     */
    private fun saveSettings() {
        Log.d(TAG, "Saving settings to repository: ${_settings.value}")
        viewModelScope.launch {
            repository.updateAllSettings(_settings.value)
        }
    }
    
    /**
     * Update a specific setting
     */
    fun updateSetting(updater: (AppSettings) -> AppSettings) {
        val oldSettings = _settings.value
        val newSettings = updater(oldSettings)
        Log.d(TAG, "Updating setting from $oldSettings to $newSettings")
        _settings.value = newSettings
        saveSettings()
    }
    
    /**
     * Toggle calendar widget visibility
     */
    fun toggleCalendarWidget() {
        Log.d(TAG, "Toggling calendar widget visibility")
        viewModelScope.launch {
            val newValue = !_settings.value.showCalendarWidget
            Log.d(TAG, "New showCalendarWidget value: $newValue")
            repository.updateShowCalendarWidget(newValue)
        }
    }
    
    /**
     * Update days to show in calendar
     */
    fun updateCalendarDaysToShow(days: Int) {
        Log.d(TAG, "Updating calendar days to show: $days")
        viewModelScope.launch {
            repository.updateCalendarDaysToShow(days)
        }
    }

    /**
     * Toggle double tap for dialer
     */
    fun toggleDoubleTapForDialer() {
        Log.d(TAG, "Toggling double tap for dialer")
        viewModelScope.launch {
            val newValue = !_settings.value.enableDoubleTapForDialer
            Log.d(TAG, "New enableDoubleTapForDialer value: $newValue")
            repository.updateEnableDoubleTapDialer(newValue)
        }
    }
    
    /**
     * Show settings dialog
     */
    fun showSettingsDialog() {
        Log.d(TAG, "Showing settings dialog")
        _showSettingsDialog.value = true
    }
    
    /**
     * Hide settings dialog
     */
    fun hideSettingsDialog() {
        Log.d(TAG, "Hiding settings dialog")
        _showSettingsDialog.value = false
        // Also hide any sub-dialogs
        _showCalendarSelection.value = false
        _showColorPaletteSelection.value = false
    }
    
    /**
     * Show calendar selection within settings
     */
    fun showCalendarSelection() {
        Log.d(TAG, "Showing calendar selection")
        // Hide settings dialog first to ensure calendar selection can be shown
        _showSettingsDialog.value = false
        // Show calendar selection
        _showCalendarSelection.value = true
    }
    
    /**
     * Hide calendar selection within settings
     */
    fun hideCalendarSelection() {
        Log.d(TAG, "Hiding calendar selection")
        _showCalendarSelection.value = false
        // Return to settings screen when closing calendar selection
        _showSettingsDialog.value = true
    }
    
    /**
     * Update color theme
     */
    fun updateColorTheme(theme: String) {
        Log.d(TAG, "Updating color theme to: $theme")
        viewModelScope.launch {
            repository.updateColorTheme(theme)
        }
    }

    /**
     * Toggle left-handed (mirrored) layout
     */
    fun toggleLeftHanded() {
        viewModelScope.launch {
            repository.updateLeftHanded(!_settings.value.leftHanded)
        }
    }

    /**
     * Show color palette selection
     */
    fun showColorPaletteSelection() {
        Log.d(TAG, "Showing color palette selection")
        // Hide settings dialog first to ensure color selection can be shown
        _showSettingsDialog.value = false
        // Show color selection
        _showColorPaletteSelection.value = true
    }
    
    /**
     * Hide color palette selection
     */
    fun hideColorPaletteSelection() {
        Log.d(TAG, "Hiding color palette selection")
        _showColorPaletteSelection.value = false
        // Return to settings screen when closing color selection
        _showSettingsDialog.value = true
    }
    
    /**
     * Show statistics screen
     */
    fun showStatisticsScreen() {
        Log.d(TAG, "Showing statistics screen")
        // Hide settings dialog first to ensure statistics can be shown
        _showSettingsDialog.value = false
        // Show statistics screen
        _showStatisticsScreen.value = true
    }
    
    /**
     * Hide statistics screen
     */
    fun hideStatisticsScreen() {
        Log.d(TAG, "Hiding statistics screen")
        _showStatisticsScreen.value = false
        // Return to settings screen when closing statistics
        _showSettingsDialog.value = true
    }
    
    // Calendar Settings Navigation
    fun showCalendarSettings() {
        Log.d(TAG, "Showing calendar settings")
        _showSettingsDialog.value = false
        _showCalendarSettings.value = true
    }
    
    fun hideCalendarSettings() {
        Log.d(TAG, "Hiding calendar settings")
        _showCalendarSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // Appearance Settings Navigation
    fun showAppearanceSettings() {
        Log.d(TAG, "Showing appearance settings")
        _showSettingsDialog.value = false
        _showAppearanceSettings.value = true
    }
    
    fun hideAppearanceSettings() {
        Log.d(TAG, "Hiding appearance settings")
        _showAppearanceSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // Gestures Settings Navigation
    fun showGesturesSettings() {
        Log.d(TAG, "Showing gestures settings")
        _showSettingsDialog.value = false
        _showGesturesSettings.value = true
    }
    
    fun hideGesturesSettings() {
        Log.d(TAG, "Hiding gestures settings")
        _showGesturesSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // Backup Settings Navigation
    fun showBackupSettings() {
        Log.d(TAG, "Showing backup settings")
        _showSettingsDialog.value = false
        _showBackupSettings.value = true
    }
    
    fun hideBackupSettings() {
        Log.d(TAG, "Hiding backup settings")
        _showBackupSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // Free Time Settings Navigation
    fun showFreeTimeSettings() {
        Log.d(TAG, "Showing free time settings")
        _showSettingsDialog.value = false
        _showFreeTimeSettings.value = true
    }
    
    fun hideFreeTimeSettings() {
        Log.d(TAG, "Hiding free time settings")
        _showFreeTimeSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // File Sync Settings Navigation
    fun showFileSyncSettings() {
        Log.d(TAG, "Showing file sync settings")
        _showSettingsDialog.value = false
        _showFileSyncSettings.value = true
    }

    fun hideFileSyncSettings() {
        Log.d(TAG, "Hiding file sync settings")
        _showFileSyncSettings.value = false
        _showSettingsDialog.value = true
    }

    // Permissions Settings Navigation
    fun showPermissionsSettings() {
        Log.d(TAG, "Showing permissions settings")
        _showSettingsDialog.value = false
        _showPermissionsSettings.value = true
    }

    fun hidePermissionsSettings() {
        Log.d(TAG, "Hiding permissions settings")
        _showPermissionsSettings.value = false
        _showSettingsDialog.value = true
    }
    
    // Calculation Calendar Selection Navigation
    fun showCalculationCalendarSelection() {
        Log.d(TAG, "Showing calculation calendar selection")
        _showFreeTimeSettings.value = false
        _showCalculationCalendarSelection.value = true
    }
    
    fun hideCalculationCalendarSelection() {
        Log.d(TAG, "Hiding calculation calendar selection")
        _showCalculationCalendarSelection.value = false
        _showFreeTimeSettings.value = true
    }
    
    /**
     * Update productivity ratio
     */
    fun updateProductivityRatio(ratio: Float) {
        Log.d(TAG, "Updating productivity ratio to: $ratio")
        viewModelScope.launch {
            repository.updateProductivityRatio(ratio)
        }
    }
    
    /**
     * Update calculation calendar IDs
     */
    fun updateCalculationCalendarIds(calendarIds: Set<Long>) {
        Log.d(TAG, "Updating calculation calendar IDs: $calendarIds")
        viewModelScope.launch {
            repository.updateCalculationCalendarIds(calendarIds)
        }
    }
    
} 