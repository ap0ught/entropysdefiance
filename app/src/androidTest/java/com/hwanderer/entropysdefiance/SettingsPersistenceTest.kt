// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hwanderer.entropysdefiance.data.SettingsRepository
import com.hwanderer.entropysdefiance.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    private val Context.testSettingsDataStore by preferencesDataStore(
        name = "test_settings"
    )

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    // Test keys that match the ones in SettingsRepository
    private val SHOW_CALENDAR_WIDGET = booleanPreferencesKey("show_calendar_widget")
    private val CALENDAR_DAYS_TO_SHOW = intPreferencesKey("calendar_days_to_show")
    private val USE_DARK_THEME = booleanPreferencesKey("use_dark_theme")
    private val ENABLE_DOUBLE_TAP_DIALER = booleanPreferencesKey("enable_double_tap_dialer")

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        repository = SettingsRepository(context)
        
        // Clear any existing preferences before each test
        runBlocking {
            context.testSettingsDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    @After
    fun tearDown() {
        // Clean up after tests
        runBlocking {
            context.testSettingsDataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }

    @Test
    fun testSettingsPersistence() = runBlocking {
        // GIVEN initial settings with default values
        val initialSettings = repository.settingsFlow.first()
        
        // THEN verify defaults
        assertEquals(false, initialSettings.showCalendarWidget)
        assertEquals(7, initialSettings.calendarDaysToShow)
        assertEquals(false, initialSettings.useDarkTheme)
        assertEquals(false, initialSettings.enableDoubleTapForDialer)

        // WHEN we update settings
        repository.updateShowCalendarWidget(true)
        repository.updateCalendarDaysToShow(14)
        repository.updateUseDarkTheme(true)
        repository.updateEnableDoubleTapDialer(true)

        // THEN verify updated values
        val updatedSettings = repository.settingsFlow.first()
        assertEquals(true, updatedSettings.showCalendarWidget)
        assertEquals(14, updatedSettings.calendarDaysToShow)
        assertEquals(true, updatedSettings.useDarkTheme)
        assertEquals(true, updatedSettings.enableDoubleTapForDialer)

        // WHEN we update all settings at once
        val newSettings = AppSettings(
            showCalendarWidget = false,
            calendarDaysToShow = 10,
            useDarkTheme = false,
            enableDoubleTapForDialer = false
        )
        repository.updateAllSettings(newSettings)

        // THEN verify all values were updated
        val finalSettings = repository.settingsFlow.first()
        assertEquals(newSettings, finalSettings)
    }

    @Test
    fun testSettingsPersistAcrossRepositoryInstances() = runBlocking {
        // GIVEN a repository instance where we set values
        repository.updateShowCalendarWidget(true)
        repository.updateCalendarDaysToShow(14)
        
        // WHEN we create a new repository instance
        val newRepository = SettingsRepository(context)
        
        // THEN the new instance should read the same values
        val settings = newRepository.settingsFlow.first()
        assertEquals(true, settings.showCalendarWidget)
        assertEquals(14, settings.calendarDaysToShow)
    }
} 