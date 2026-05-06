package com.hwanderer.entropysdefiance

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hwanderer.entropysdefiance.data.SettingsRepository
import com.hwanderer.entropysdefiance.model.AppSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SettingsRepositoryTest {

    // Test keys that match the ones in SettingsRepository
    private val SHOW_CALENDAR_WIDGET = booleanPreferencesKey("show_calendar_widget")
    private val CALENDAR_DAYS_TO_SHOW = intPreferencesKey("calendar_days_to_show")
    private val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    private val ENABLE_DOUBLE_TAP_DIALER = booleanPreferencesKey("enable_double_tap_dialer")

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()
    
    private fun createTestDataStore(scope: TestScope): DataStore<Preferences> {
        val testFile = tmpFolder.newFile("test_settings.preferences_pb")
        return PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { testFile }
        )
    }

    @Test
    fun `test settings flow emits correct defaults when preferences are empty`() = runTest {
        // Given a test DataStore and repository
        val dataStore = createTestDataStore(this)
        val repository = SettingsRepository(dataStore)
        
        // When we collect from settingsFlow with an empty DataStore
        val result = repository.settingsFlow.first()
        
        // Then we should get default values (matching SettingsRepository defaults)
        val expected = AppSettings(
            showCalendarWidget = true,  // default is true in SettingsRepository
            calendarDaysToShow = 7,
            useDarkTheme = false,
            enableDoubleTapForDialer = true,  // default is true in SettingsRepository
            colorTheme = "Green",
            selectedCalendarIds = emptySet(),
            enableAutomaticBackup = false,
            backupIntervalHours = 24
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test settings flow emits correct values from preferences`() = runTest {
        // Given a test DataStore and repository with custom values
        val dataStore = createTestDataStore(this)
        val repository = SettingsRepository(dataStore)
        
        dataStore.edit { preferences ->
            preferences[SHOW_CALENDAR_WIDGET] = false
            preferences[CALENDAR_DAYS_TO_SHOW] = 14
            preferences[USE_DARK_THEME] = true
            preferences[ENABLE_DOUBLE_TAP_DIALER] = false
        }
        
        // When we collect from settingsFlow
        val result = repository.settingsFlow.first()
        
        // Then we should get our custom values (with defaults for other fields)
        val expected = AppSettings(
            showCalendarWidget = false,
            calendarDaysToShow = 14,
            useDarkTheme = true,
            enableDoubleTapForDialer = false,
            colorTheme = "Green",  // default value
            selectedCalendarIds = emptySet(),  // default value
            enableAutomaticBackup = false,  // default value
            backupIntervalHours = 24  // default value
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test updateShowCalendarWidget updates the correct preference`() = runTest {
        // Given a test DataStore and repository
        val dataStore = createTestDataStore(this)
        val repository = SettingsRepository(dataStore)
        
        // Initial state with default value (true)
        val initialSettings = repository.settingsFlow.first()
        assertEquals(true, initialSettings.showCalendarWidget)
        
        // When - update to false
        repository.updateShowCalendarWidget(false)
        
        // Then - verify the preference was updated
        val updatedSettings = repository.settingsFlow.first()
        assertEquals(false, updatedSettings.showCalendarWidget)
    }

    @Test
    fun `test updateAllSettings updates all preferences correctly`() = runTest {
        // Given a test DataStore and repository
        val dataStore = createTestDataStore(this)
        val repository = SettingsRepository(dataStore)
        
        // When - update all settings
        val newSettings = AppSettings(
            showCalendarWidget = false,
            calendarDaysToShow = 10,
            useDarkTheme = true,
            enableDoubleTapForDialer = false,
            colorTheme = "Blue",
            selectedCalendarIds = setOf(1L, 2L),
            enableAutomaticBackup = true,
            backupIntervalHours = 12
        )
        repository.updateAllSettings(newSettings)
        
        // Then - verify all preferences were updated
        val updatedSettings = repository.settingsFlow.first()
        assertEquals(newSettings, updatedSettings)
    }
} 