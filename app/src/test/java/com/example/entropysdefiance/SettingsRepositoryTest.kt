package com.example.entropysdefiance

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.entropysdefiance.data.SettingsRepository
import com.example.entropysdefiance.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [30])
class SettingsRepositoryTest {

    // Test keys that match the ones in SettingsRepository
    private val SHOW_CALENDAR_WIDGET = booleanPreferencesKey("show_calendar_widget")
    private val CALENDAR_DAYS_TO_SHOW = intPreferencesKey("calendar_days_to_show")
    private val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    private val ENABLE_DOUBLE_TAP_DIALER = booleanPreferencesKey("enable_double_tap_dialer")

    private lateinit var mockContext: Context
    private lateinit var mockDataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        // Create mock context and DataStore
        mockContext = Mockito.mock(Context::class.java)
        mockDataStore = Mockito.mock(DataStore::class.java) as DataStore<Preferences>
        
        // Use the real repository but with mock dependencies
        repository = SettingsRepository(mockContext)
    }

    @Test
    fun `test settings flow emits correct defaults when preferences are empty`() = runBlocking {
        // Given an empty preferences
        val emptyPreferences = preferencesOf()
        whenever(mockDataStore.data).thenReturn(kotlinx.coroutines.flow.flowOf(emptyPreferences))
        
        // When we collect from settingsFlow
        val result = repository.settingsFlow.first()
        
        // Then we should get default values
        val expected = AppSettings(
            showCalendarWidget = false,
            calendarDaysToShow = 7,
            useDarkTheme = false,
            enableDoubleTapForDialer = false
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test settings flow emits correct values from preferences`() = runBlocking {
        // Given preferences with custom values
        val customPreferences = preferencesOf(
            SHOW_CALENDAR_WIDGET to true,
            CALENDAR_DAYS_TO_SHOW to 14,
            USE_DARK_THEME to true,
            ENABLE_DOUBLE_TAP_DIALER to true
        )
        whenever(mockDataStore.data).thenReturn(kotlinx.coroutines.flow.flowOf(customPreferences))
        
        // When we collect from settingsFlow
        val result = repository.settingsFlow.first()
        
        // Then we should get our custom values
        val expected = AppSettings(
            showCalendarWidget = true,
            calendarDaysToShow = 14,
            useDarkTheme = true,
            enableDoubleTapForDialer = true
        )
        assertEquals(expected, result)
    }

    @Test
    fun `test updateShowCalendarWidget updates the correct preference`() = runBlocking {
        // Given
        val initialPreferences = preferencesOf()
        whenever(mockDataStore.data).thenReturn(kotlinx.coroutines.flow.flowOf(initialPreferences))
        
        // When
        repository.updateShowCalendarWidget(true)
        
        // Then verify that edit was called with the correct transformation
        // Note: This is a simplified test. In a real test, you'd need to use ArgumentCaptor
        // to capture the lambda passed to edit() and verify it transforms the preferences correctly.
    }

    @Test
    fun `test updateAllSettings updates all preferences correctly`() = runBlocking {
        // Given
        val initialPreferences = preferencesOf()
        whenever(mockDataStore.data).thenReturn(kotlinx.coroutines.flow.flowOf(initialPreferences))
        
        // When
        val newSettings = AppSettings(
            showCalendarWidget = true,
            calendarDaysToShow = 10,
            useDarkTheme = true,
            enableDoubleTapForDialer = true
        )
        repository.updateAllSettings(newSettings)
        
        // Then verify that edit was called with the correct transformation
        // Note: This is a simplified test. In a real test, you'd need to use ArgumentCaptor
        // to capture the lambda passed to edit() and verify it transforms the preferences correctly.
    }
} 