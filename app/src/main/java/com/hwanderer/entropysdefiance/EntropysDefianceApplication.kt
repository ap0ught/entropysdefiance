// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance

import android.app.Application
import android.util.Log
import com.hwanderer.entropysdefiance.data.SettingsRepository
import com.hwanderer.entropysdefiance.utils.ZenModeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main application class for initializing app-wide components
 */
class EntropysDefianceApplication : Application() {
    
    // Create an application scope for long-running coroutines
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // Lazy initialize the SettingsRepository to ensure it's created at app startup
    val settingsRepository by lazy { 
        SettingsRepository(applicationContext)
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("Application", "Initializing EntropysDefianceApplication")
        
        // Initialize settings repository
        applicationScope.launch {
            Log.d("Application", "Initializing SettingsRepository")
            // Access the settingsRepository to initialize it
            val repository = settingsRepository
            
            // Dump initial DataStore contents for debugging
            repository.dumpDataStoreContents()
        }
        
        // If a previous run was killed mid-Zen-Mode, Do Not Disturb is a *global system*
        // setting and would still be on with nothing left to turn it off. Zen Mode state is
        // never restored across process death, so an ownership flag surviving in prefs can
        // only mean exactly that. No-op in the normal case.
        ZenModeManager(this).restoreDnd()

        Log.d("Application", "EntropysDefianceApplication initialized")
    }
} 