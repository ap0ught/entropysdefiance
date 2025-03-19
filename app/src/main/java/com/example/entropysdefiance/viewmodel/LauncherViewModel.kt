package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps
    
    // SharedPreferences to store app usage counts
    private val appUsagePrefs: SharedPreferences = application.getSharedPreferences(
        "app_usage_prefs", Context.MODE_PRIVATE
    )

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val pm = getApplication<Application>().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            val appList = resolveInfoList.map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    appName = resolveInfo.loadLabel(pm).toString(),
                    icon = resolveInfo.loadIcon(pm),
                    launchIntent = resolveInfo.activityInfo.name
                )
            }
            
            // Sort by usage count (most used first)
            val sortedAppList = appList.sortedByDescending { app -> 
                appUsagePrefs.getInt(app.packageName, 0)
            }

            _apps.value = sortedAppList
        }
    }

    fun launchApp(packageName: String, launchIntent: String) {
        // Increment usage count for this app
        val currentCount = appUsagePrefs.getInt(packageName, 0)
        appUsagePrefs.edit().putInt(packageName, currentCount + 1).apply()
        
        // Launch the app
        val context = getApplication<Application>()
        val intent = Intent().apply {
            setClassName(packageName, launchIntent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        
        // Refresh the app list with updated usage counts
        loadApps()
    }

    fun launchCalendarApp(customCalendarName: String = "Calendar") {
        val context = getApplication<Application>()
        
        // First try to find the custom calendar app by name or package
        val customCalendarApp = apps.value.firstOrNull { app -> 
            app.appName.contains(customCalendarName, ignoreCase = true) || 
            app.packageName.contains(customCalendarName.lowercase().replace(" ", ""), ignoreCase = true)
        }
        
        if (customCalendarApp != null) {
            // Launch the custom calendar app if found
            launchApp(customCalendarApp.packageName, customCalendarApp.launchIntent)
            return
        }
        
        try {
            // Fall back to the system calendar intent if custom app not found
            val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calendarIntent)
        } catch (e: Exception) {
            // Last resort: try to find any calendar app
            val anyCalendarApp = apps.value.firstOrNull { 
                it.packageName.contains("calendar", ignoreCase = true) || 
                it.appName.contains("calendar", ignoreCase = true) 
            }
            
            if (anyCalendarApp != null) {
                launchApp(anyCalendarApp.packageName, anyCalendarApp.launchIntent)
            }
        }
    }

    fun findPhoneApp(): AppInfo? {
        // Look for common phone app patterns
        return apps.value.firstOrNull { app -> 
            // Print each app name and package for debugging
            println("App: ${app.appName}, Package: ${app.packageName}")
            
            // Check for common phone app identifiers
            app.appName.equals("Phone", ignoreCase = true) ||
            app.appName.equals("Dialer", ignoreCase = true) ||
            app.packageName.contains("dialer", ignoreCase = true) ||
            app.packageName.contains("phone", ignoreCase = true) ||
            app.packageName.contains("call", ignoreCase = true)
        }
    }

    fun launchPhoneApp() {
        val context = getApplication<Application>()
        
        // First try to find the phone app in our loaded apps
        val phoneApp = findPhoneApp()
        
        if (phoneApp != null) {
            println("Found phone app: ${phoneApp.appName}, package: ${phoneApp.packageName}")
            launchApp(phoneApp.packageName, phoneApp.launchIntent)
            return
        }
        
        // If we can't find it in our apps, try a direct intent approach
        try {
            println("Trying direct dial intent")
            val dialIntent = Intent(Intent.ACTION_DIAL)
            dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            println("Failed to launch phone app: ${e.message}")
            // We've tried our best, nothing more we can do
        }
    }
} 