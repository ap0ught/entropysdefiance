package com.example.entropysdefiance

import android.app.Application
import android.util.Log
import com.example.entropysdefiance.data.SettingsRepository
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