package com.example.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing application settings
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    // State for settings
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    
    // State for settings dialog visibility
    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()
    
    // State for calendar selection visibility within settings
    private val _showCalendarSelection = MutableStateFlow(false)
    val showCalendarSelection: StateFlow<Boolean> = _showCalendarSelection.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load settings from persistent storage
     * (For now, uses default values, but would be expanded to use SharedPreferences or DataStore)
     */
    private fun loadSettings() {
        viewModelScope.launch {
            // TODO: Load from persistent storage when implemented
            // For now, just use defaults
            _settings.value = AppSettings()
        }
    }
    
    /**
     * Save settings to persistent storage
     */
    private fun saveSettings() {
        viewModelScope.launch {
            // TODO: Save to persistent storage when implemented
            // For now, just keep in memory
        }
    }
    
    /**
     * Update a specific setting
     */
    fun updateSetting(updater: (AppSettings) -> AppSettings) {
        _settings.value = updater(_settings.value)
        saveSettings()
    }
    
    /**
     * Toggle calendar widget visibility
     */
    fun toggleCalendarWidget() {
        updateSetting { it.copy(showCalendarWidget = !it.showCalendarWidget) }
    }
    
    /**
     * Update days to show in calendar
     */
    fun updateCalendarDaysToShow(days: Int) {
        updateSetting { it.copy(calendarDaysToShow = days) }
    }
    
    /**
     * Toggle dark theme
     */
    fun toggleDarkTheme() {
        updateSetting { it.copy(useDarkTheme = !it.useDarkTheme) }
    }
    
    /**
     * Toggle double tap for dialer
     */
    fun toggleDoubleTapForDialer() {
        updateSetting { it.copy(enableDoubleTapForDialer = !it.enableDoubleTapForDialer) }
    }
    
    /**
     * Show settings dialog
     */
    fun showSettingsDialog() {
        _showSettingsDialog.value = true
    }
    
    /**
     * Hide settings dialog
     */
    fun hideSettingsDialog() {
        _showSettingsDialog.value = false
        // Also hide any sub-dialogs
        _showCalendarSelection.value = false
    }
    
    /**
     * Show calendar selection within settings
     */
    fun showCalendarSelection() {
        _showCalendarSelection.value = true
    }
    
    /**
     * Hide calendar selection within settings
     */
    fun hideCalendarSelection() {
        _showCalendarSelection.value = false
    }
} 