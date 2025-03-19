package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.data.CalendarRepository
import com.example.entropysdefiance.model.CalendarEvent
import com.example.entropysdefiance.model.DeviceCalendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.Manifest

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(application)
    
    // State for events
    private val _events = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val events: StateFlow<List<CalendarEvent>> = _events.asStateFlow()
    
    // State for loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // State for permission
    private val _hasCalendarPermission = mutableStateOf(checkCalendarPermission())
    val hasCalendarPermission: State<Boolean> = _hasCalendarPermission
    
    // State for available calendars
    private val _availableCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val availableCalendars: StateFlow<List<DeviceCalendar>> = _availableCalendars.asStateFlow()
    
    // State for calendar selection dialog visibility
    private val _showCalendarSelection = MutableStateFlow(false)
    val showCalendarSelection: StateFlow<Boolean> = _showCalendarSelection.asStateFlow()
    
    /**
     * Check if we have calendar permission
     */
    private fun checkCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Update permission state (call from UI after permission request)
     */
    fun updatePermissionState() {
        _hasCalendarPermission.value = checkCalendarPermission()
        if (_hasCalendarPermission.value) {
            loadCalendars()
            loadEvents()
        }
    }
    
    /**
     * Load available calendars from the device
     */
    fun loadCalendars() {
        if (!hasCalendarPermission.value) {
            return
        }
        
        viewModelScope.launch {
            try {
                val calendars = repository.getAvailableCalendars()
                _availableCalendars.value = calendars
            } catch (e: Exception) {
                // Handle error (e.g., log it)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load calendar events with default 7 day window
     */
    fun loadEvents(daysInFuture: Int = 7) {
        if (!hasCalendarPermission.value) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get selected calendar IDs
                val selectedCalendarIds = _availableCalendars.value
                    .filter { it.selected }
                    .map { it.id }
                
                val calendarEvents = repository.getEvents(
                    daysInFuture = daysInFuture,
                    selectedCalendarIds = selectedCalendarIds
                )
                _events.value = calendarEvents
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle selection for a specific calendar
     */
    fun toggleCalendarSelection(calendarId: Long) {
        val currentList = _availableCalendars.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == calendarId }
        
        if (index != -1) {
            val calendar = currentList[index]
            // Create a new calendar object with toggled selection
            currentList[index] = calendar.copy(selected = !calendar.selected)
            _availableCalendars.value = currentList
            
            // Reload events with new selection
            loadEvents()
        }
    }
    
    /**
     * Get the app settings intent to direct users to enable permissions
     */
    fun getAppSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", getApplication<Application>().packageName, null)
        intent.data = uri
        return intent
    }
    
    /**
     * Show calendar selection dialog
     */
    fun showCalendarSelectionDialog() {
        _showCalendarSelection.value = true
    }
    
    /**
     * Hide calendar selection dialog
     */
    fun hideCalendarSelectionDialog() {
        _showCalendarSelection.value = false
    }
    
    /**
     * Initialize - call when view is created
     */
    fun initialize() {
        if (hasCalendarPermission.value) {
            loadCalendars()
            loadEvents()
        }
    }
} 