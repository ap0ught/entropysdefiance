// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance

import android.app.Application
import android.util.Log
import com.hwanderer.entropysdefiance.data.SettingsRepository
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
        
        Log.d("Application", "EntropysDefianceApplication initialized")
    }
} 