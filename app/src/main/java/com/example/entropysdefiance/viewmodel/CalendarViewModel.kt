package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.EntropysDefianceApplication
import com.example.entropysdefiance.data.CalendarRepository
import com.example.entropysdefiance.data.SettingsRepository
import com.example.entropysdefiance.model.CalendarEvent
import com.example.entropysdefiance.model.DeviceCalendar
import com.example.entropysdefiance.model.FreeTimeMetric
import com.example.entropysdefiance.model.TimeHorizon
import com.example.entropysdefiance.utils.CalendarTimeCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.Manifest
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val TAG = "CalendarViewModel"

class CalendarViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CalendarRepository(application)
    private val settingsRepository = (application as EntropysDefianceApplication).settingsRepository
    
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
    
    // State for free time metric
    private val _freeTimeMetric = MutableStateFlow<FreeTimeMetric?>(null)
    val freeTimeMetric: StateFlow<FreeTimeMetric?> = _freeTimeMetric.asStateFlow()
    
    // Current time horizon for free time calculation
    private val _currentTimeHorizon = MutableStateFlow<TimeHorizon>(TimeHorizon.Today)
    val currentTimeHorizon: StateFlow<TimeHorizon> = _currentTimeHorizon.asStateFlow()
    
    // For managing update jobs
    private var periodicUpdateJob: Job? = null
    private var nextEventUpdateJob: Job? = null
    private var currentCalendarDays = 7 // Default days to look ahead
    
    // Saved selected calendar IDs from settings
    private var savedSelectedCalendarIds = emptySet<Long>()
    
    // Calendar change receiver for external changes
    private val calendarChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // When calendar changes are detected, refresh both calendars and events
            Log.d(TAG, "Calendar change detected, reloading data")
            viewModelScope.launch {
                try {
                    // Always reload settings first to get the latest selection state
                    val settings = settingsRepository.settingsFlow.first()
                    savedSelectedCalendarIds = settings.selectedCalendarIds
                    Log.d(TAG, "Receiver: Retrieved saved calendar selection with ${savedSelectedCalendarIds.size} calendars")
                    
                    // Then reload calendars with updated selection state
                    loadCalendars()
                    
                    // Finally load events with the updated state
                    loadEvents(currentCalendarDays)
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing data after calendar change", e)
                    // Fall back to regular reload
                    loadEvents(currentCalendarDays)
                }
            }
        }
    }
    
    init {
        registerCalendarChangeReceiver()
        // Start observing settings to get updated selected calendar IDs
        observeSettings()
    }
    
    /**
     * Observe settings changes to get selected calendar IDs
     */
    private fun observeSettings() {
        viewModelScope.launch {
            var isFirstLoad = true
            settingsRepository.settingsFlow.collect { settings ->
                Log.d(TAG, "Received settings update with ${settings.selectedCalendarIds.size} calendars")
                
                val oldSelectedIds = savedSelectedCalendarIds
                savedSelectedCalendarIds = settings.selectedCalendarIds
                
                // Update currentCalendarDays when settings change
                if (currentCalendarDays != settings.calendarDaysToShow) {
                    currentCalendarDays = settings.calendarDaysToShow
                    // Reload events with new days setting
                    loadEvents(currentCalendarDays)
                }
                
                // If this is the first load after app start and we have available calendars,
                // immediately persist any default selections to avoid stale data
                if (isFirstLoad) {
                    isFirstLoad = false
                    
                    // If we have available calendars but no saved selections,
                    // persist whatever default selection state the calendars have
                    val currentCalendars = _availableCalendars.value
                    if (currentCalendars.isNotEmpty() && savedSelectedCalendarIds.isEmpty()) {
                        val defaultSelectedIds = currentCalendars
                            .filter { it.selected }
                            .map { it.id }
                            .toSet()
                            
                        if (defaultSelectedIds.isNotEmpty()) {
                            Log.d(TAG, "First app load: Persisting default calendar selection: $defaultSelectedIds")
                            settingsRepository.updateSelectedCalendarIds(defaultSelectedIds)
                            savedSelectedCalendarIds = defaultSelectedIds
                            // Skip the rest of the processing for this settings update
                            return@collect
                        }
                    }
                }
                
                // If selection has changed, update calendar state and reload events
                if (oldSelectedIds != savedSelectedCalendarIds) {
                    // If we already have calendars loaded, update their selection state
                    updateCalendarSelectionState()
                    
                    // Also reload events with the new selection
                    loadEvents(currentCalendarDays)
                }
            }
        }
    }
    
    /**
     * Update the selection state of loaded calendars based on saved settings
     */
    private fun updateCalendarSelectionState() {
        val currentCalendars = _availableCalendars.value
        if (currentCalendars.isNotEmpty() && savedSelectedCalendarIds.isNotEmpty()) {
            Log.d(TAG, "Updating calendar selection states from settings")
            val updatedCalendars = currentCalendars.map { calendar ->
                calendar.copy(selected = calendar.id in savedSelectedCalendarIds)
            }
            _availableCalendars.value = updatedCalendars
        }
    }
    
    /**
     * Register for calendar content change broadcasts
     */
    private fun registerCalendarChangeReceiver() {
        val filter = IntentFilter().apply {
            addAction(CalendarContract.ACTION_EVENT_REMINDER)
            addAction(Intent.ACTION_PROVIDER_CHANGED)
            addDataScheme("content")
            addDataAuthority("com.android.calendar", null)
        }
        getApplication<Application>().registerReceiver(
            calendarChangeReceiver, 
            filter,
            Context.RECEIVER_NOT_EXPORTED
        )
    }
    
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
            startPeriodicUpdates()
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
                // First get saved settings - do this first to ensure we have the latest data
                val settings = settingsRepository.settingsFlow.first()
                savedSelectedCalendarIds = settings.selectedCalendarIds
                Log.d(TAG, "Retrieved saved calendar selection with ${savedSelectedCalendarIds.size} calendars")
                
                // Then load calendars
                val calendars = repository.getAvailableCalendars()
                
                if (calendars.isEmpty()) {
                    Log.d(TAG, "No calendars found on device")
                    return@launch
                }
                
                // Apply saved selection state if available
                if (savedSelectedCalendarIds.isNotEmpty()) {
                    Log.d(TAG, "Applying saved selection state to ${calendars.size} calendars")
                    // Create updated calendars with selection state from saved settings
                    val updatedCalendars = calendars.map { calendar ->
                        calendar.copy(selected = calendar.id in savedSelectedCalendarIds)
                    }
                    _availableCalendars.value = updatedCalendars
                } else {
                    // If no saved selection, use the default selection from the device
                    // but don't save yet - wait for user to make explicit changes
                    Log.d(TAG, "Using default selection from device")
                    _availableCalendars.value = calendars
                    
                    // Log the default selection state for debugging
                    val defaultSelectedIds = calendars
                        .filter { it.selected }
                        .map { it.id }
                    Log.d(TAG, "Default calendar selection: $defaultSelectedIds")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading calendars", e)
            }
        }
    }
    
    /**
     * Load calendar events with default 7 day window
     */
    fun loadEvents(daysInFuture: Int = currentCalendarDays) {
        if (!hasCalendarPermission.value) {
            Log.d(TAG, "Cannot load events: No calendar permission")
            return
        }
        
        currentCalendarDays = daysInFuture
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get selected calendar IDs from the current calendar list
                val selectedCalendarIds = _availableCalendars.value
                    .filter { it.selected }
                    .map { it.id }
                
                // For safety, also use our saved selection state when calendar list is empty
                val idsToUse = if (selectedCalendarIds.isEmpty() && savedSelectedCalendarIds.isNotEmpty()) {
                    Log.d(TAG, "Using saved calendar IDs for events as UI list is empty: ${savedSelectedCalendarIds.size} calendars")
                    savedSelectedCalendarIds.toList()
                } else {
                    Log.d(TAG, "Loading events with ${selectedCalendarIds.size} selected calendars")
                    selectedCalendarIds
                }
                
                val calendarEvents = repository.getEvents(
                    daysInFuture = daysInFuture,
                    selectedCalendarIds = idsToUse
                )
                _events.value = calendarEvents
                
                // Calculate free time metric after loading events
                calculateFreeTimeMetric()
                
                // Schedule next update based on closest event start/end time
                scheduleNextUpdateForEventTimes(calendarEvents)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading events", e)
                // Clear events in case of error to avoid showing stale data
                _events.value = emptyList()
                _freeTimeMetric.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Calculate free time metric based on current events and settings
     */
    private fun calculateFreeTimeMetric() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.settingsFlow.first()
                val calculationCalendarIds = settings.calculationCalendarIds
                val productivityRatio = settings.productivityRatio
                
                // If no calculation calendars are selected, use all selected calendars
                val idsToUse = if (calculationCalendarIds.isEmpty()) {
                    Log.d(TAG, "No calculation calendars set, using display calendars")
                    settings.selectedCalendarIds
                } else {
                    calculationCalendarIds
                }
                
                // Filter events by calculation calendar IDs
                val eventsForCalculation = if (idsToUse.isNotEmpty()) {
                    _events.value.filter { event ->
                        event.calendarId in idsToUse
                    }
                } else {
                    // If no calendars selected at all, use all events
                    _events.value
                }
                
                Log.d(TAG, "Calculating free time with ${eventsForCalculation.size} events " +
                           "from ${idsToUse.size} calendars, " +
                           "productivity ratio: $productivityRatio, " +
                           "time horizon: ${_currentTimeHorizon.value}")
                
                val metric = CalendarTimeCalculator.calculateFreeTime(
                    events = eventsForCalculation,
                    timeHorizon = _currentTimeHorizon.value,
                    productivityRatio = productivityRatio
                )
                
                _freeTimeMetric.value = metric
                
                Log.d(TAG, "Free time calculated: ${metric.productiveHoursAvailable} productive hours available")
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating free time metric", e)
                _freeTimeMetric.value = null
            }
        }
    }
    
    /**
     * Update the current time horizon for free time calculation
     */
    fun updateTimeHorizon(timeHorizon: TimeHorizon) {
        if (_currentTimeHorizon.value != timeHorizon) {
            _currentTimeHorizon.value = timeHorizon
            // Recalculate free time metric with new time horizon
            calculateFreeTimeMetric()
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
            val newSelectionState = !calendar.selected
            currentList[index] = calendar.copy(selected = newSelectionState)
            _availableCalendars.value = currentList
            
            // Save the updated selection to settings
            val selectedIds = currentList
                .filter { it.selected }
                .map { it.id }
                .toSet()
            
            Log.d(TAG, "Saving updated calendar selection: $selectedIds")
            viewModelScope.launch {
                try {
                    // Update both our cached value and the persistent storage
                    savedSelectedCalendarIds = selectedIds
                    settingsRepository.updateSelectedCalendarIds(selectedIds)
                    
                    // Log for debugging
                    Log.d(TAG, "Successfully saved calendar selection with ${selectedIds.size} calendars")
                    
                    // Also reload events to reflect the changes
                    loadEvents(currentCalendarDays)
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving calendar selection", e)
                }
            }
        } else {
            Log.e(TAG, "Tried to toggle non-existent calendar with ID: $calendarId")
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
     * Start periodic updates every 15 minutes
     */
    private fun startPeriodicUpdates() {
        // Cancel any existing job first
        periodicUpdateJob?.cancel()
        
        // Start a new periodic update job
        periodicUpdateJob = viewModelScope.launch {
            while (true) {
                delay(TimeUnit.MINUTES.toMillis(15)) // Update every 15 minutes
                loadEvents(currentCalendarDays)
            }
        }
    }
    
    /**
     * Schedule next update based on event start/end times
     */
    private fun scheduleNextUpdateForEventTimes(events: List<CalendarEvent>) {
        // Cancel any existing job first
        nextEventUpdateJob?.cancel()
        
        if (events.isEmpty()) return
        
        val now = LocalDateTime.now()
        
        // Find the next event time change (start or end)
        val nextEventTime = events
            .flatMap { event ->
                // Consider both start and end times that are in the future
                listOfNotNull(
                    event.startTime.takeIf { it.isAfter(now) },
                    event.endTime.takeIf { it.isAfter(now) }
                )
            }
            .minByOrNull { it.toEpochSecond(ZoneOffset.UTC) }
        
        nextEventTime?.let { eventTime ->
            // Calculate delay until the next event time
            val delayMillis = ChronoUnit.MILLIS.between(now, eventTime)
            
            // If the delay is positive and less than 15 minutes, schedule an update
            if (delayMillis > 0 && delayMillis < TimeUnit.MINUTES.toMillis(15)) {
                nextEventUpdateJob = viewModelScope.launch {
                    delay(delayMillis)
                    loadEvents(currentCalendarDays)
                }
            }
        }
    }
    
    /**
     * Initialize - call when view is created
     */
    fun initialize() {
        if (hasCalendarPermission.value) {
            // First fetch settings, then load calendars, and finally load events
            viewModelScope.launch {
                try {
                    // Get saved settings first
                    val settings = settingsRepository.settingsFlow.first()
                    savedSelectedCalendarIds = settings.selectedCalendarIds
                    Log.d(TAG, "Initialize: Retrieved saved calendar selection with ${savedSelectedCalendarIds.size} calendars")
                    
                    // Load calendars with the settings
                    loadCalendars()
                    
                    // Only then load events once calendars are properly initialized with selection state
                    loadEvents(settings.calendarDaysToShow)
                    
                    // Start periodic updates 
                    startPeriodicUpdates()
                } catch (e: Exception) {
                    Log.e(TAG, "Error during initialization", e)
                    // If there's an error, still try to load data with default selection
                    loadCalendars()
                    loadEvents()
                    startPeriodicUpdates()
                }
            }
        }
    }
    
    /**
     * Select all calendars
     */
    fun selectAllCalendars() {
        val currentList = _availableCalendars.value
        if (currentList.isEmpty()) {
            Log.d(TAG, "No calendars to select")
            return
        }
        
        // Create a new list with all calendars selected
        val updatedList = currentList.map { it.copy(selected = true) }
        _availableCalendars.value = updatedList
        
        // Get the IDs of all calendars
        val allCalendarIds = updatedList.map { it.id }.toSet()
        
        // Save the selection
        viewModelScope.launch {
            try {
                // Update both our cached value and the persistent storage
                savedSelectedCalendarIds = allCalendarIds
                settingsRepository.updateSelectedCalendarIds(allCalendarIds)
                
                // Log for debugging
                Log.d(TAG, "Selected all ${allCalendarIds.size} calendars")
                
                // Reload events to reflect the changes
                loadEvents(currentCalendarDays)
            } catch (e: Exception) {
                Log.e(TAG, "Error selecting all calendars", e)
            }
        }
    }
    
    /**
     * Deselect all calendars
     */
    fun deselectAllCalendars() {
        val currentList = _availableCalendars.value
        if (currentList.isEmpty()) {
            Log.d(TAG, "No calendars to deselect")
            return
        }
        
        // Create a new list with all calendars deselected
        val updatedList = currentList.map { it.copy(selected = false) }
        _availableCalendars.value = updatedList
        
        // Save the empty selection
        viewModelScope.launch {
            try {
                // Update both our cached value and the persistent storage
                savedSelectedCalendarIds = emptySet()
                settingsRepository.updateSelectedCalendarIds(emptySet())
                
                // Log for debugging
                Log.d(TAG, "Deselected all calendars")
                
                // Reload events to reflect the changes
                loadEvents(currentCalendarDays)
            } catch (e: Exception) {
                Log.e(TAG, "Error deselecting all calendars", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up when ViewModel is destroyed
        periodicUpdateJob?.cancel()
        nextEventUpdateJob?.cancel()
        try {
            getApplication<Application>().unregisterReceiver(calendarChangeReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
} 