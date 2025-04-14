package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.EntropysDefianceApplication
import com.example.entropysdefiance.data.SettingsRepository
import com.example.entropysdefiance.model.AppSettings
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
     * Toggle dark theme
     */
    fun toggleDarkTheme() {
        Log.d(TAG, "Toggling dark theme")
        viewModelScope.launch {
            val newValue = !_settings.value.useDarkTheme
            Log.d(TAG, "New useDarkTheme value: $newValue")
            repository.updateUseDarkTheme(newValue)
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
} 