package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Process
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val context = getApplication<Application>()
            // Enumerate apps and shortcuts off the main thread. queryIntentActivities and
            // loadLabel do IPC/resource work per app; running them on the main thread caused
            // launcher jank on cold start (this runs in init) and after each launch.
            val sortedAppList = withContext(Dispatchers.Default) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null)
                intent.addCategory(Intent.CATEGORY_LAUNCHER)

                val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                val appList = resolveInfoList.map { resolveInfo ->
                    AppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        appName = resolveInfo.loadLabel(pm).toString(),
                        launchIntent = resolveInfo.activityInfo.name
                    )
                }

                // Query pinned shortcuts (PWAs and website shortcuts)
                val shortcutList = loadPinnedShortcuts(context)

                // Sort by usage count (most used first)
                (appList + shortcutList).sortedByDescending { app ->
                    appUsagePrefs.getInt(app.usageKey, 0)
                }
            }

            _apps.value = sortedAppList
        }
    }

    private fun loadPinnedShortcuts(context: Context): List<AppInfo> {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val query = LauncherApps.ShortcutQuery().apply {
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }
            launcherApps.getShortcuts(query, Process.myUserHandle())?.map { shortcut ->
                AppInfo(
                    packageName = shortcut.`package`,
                    appName = shortcut.shortLabel?.toString()
                        ?: shortcut.longLabel?.toString()
                        ?: shortcut.`package`,
                    launchIntent = "",
                    shortcutId = shortcut.id
                )
            } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("LauncherViewModel", "Failed to load pinned shortcuts: ${e.message}", e)
            emptyList()
        }
    }

    fun refreshApps() {
        loadApps()
    }

    fun launchApp(packageName: String, launchIntent: String, shortcutId: String? = null) {
        val context = getApplication<Application>()

        if (shortcutId != null) {
            // Launch pinned shortcut (PWA / website shortcut)
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            try {
                launcherApps.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
                val key = "shortcut:$packageName:$shortcutId"
                appUsagePrefs.edit().putInt(key, appUsagePrefs.getInt(key, 0) + 1).apply()
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to launch shortcut: ${e.message}", e)
            }
            loadApps()
            return
        }

        // Increment usage count for this app
        val currentCount = appUsagePrefs.getInt(packageName, 0)
        appUsagePrefs.edit().putInt(packageName, currentCount + 1).apply()

        // Launch the app
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
        
        android.util.Log.d("LauncherViewModel", "Attempting to launch calendar app: $customCalendarName")
        
        // First try to find the custom calendar app by name or package
        var customCalendarApp = apps.value.firstOrNull { app -> 
            app.appName.contains(customCalendarName, ignoreCase = true) || 
            app.packageName.contains(customCalendarName.lowercase().replace(" ", ""), ignoreCase = true)
        }
        
        if (customCalendarApp != null) {
            // Launch the custom calendar app if found
            android.util.Log.d("LauncherViewModel", "Found custom calendar app: ${customCalendarApp.appName}, launching it")
            launchApp(customCalendarApp.packageName, customCalendarApp.launchIntent)
            return
        }
        
        // If specific calendar app not found, try looking for any calendar app
        android.util.Log.d("LauncherViewModel", "Custom calendar app not found, looking for any calendar app")
        customCalendarApp = apps.value.firstOrNull { app -> 
            app.appName.equals("Calendar", ignoreCase = true) ||
            app.appName.contains("Calendar", ignoreCase = true) ||
            app.packageName.contains("calendar", ignoreCase = true) ||
            app.packageName.contains("com.google.android.calendar", ignoreCase = true) ||
            app.packageName.contains("com.android.calendar", ignoreCase = true) ||
            app.packageName.contains("samsung.android.calendar", ignoreCase = true) ||
            app.packageName.contains("huawei.calendar", ignoreCase = true) ||
            app.packageName.contains("xiaomi.calendar", ignoreCase = true) ||
            app.packageName.contains("oppo.calendar", ignoreCase = true) ||
            app.packageName.contains("vivo.calendar", ignoreCase = true) ||
            app.packageName.contains("oneplus.calendar", ignoreCase = true)
        }
        
        if (customCalendarApp != null) {
            // Launch the found calendar app
            android.util.Log.d("LauncherViewModel", "Found general calendar app: ${customCalendarApp.appName}, launching it")
            launchApp(customCalendarApp.packageName, customCalendarApp.launchIntent)
            return
        }
        
        try {
            // Fall back to the system calendar intent if custom app not found
            android.util.Log.d("LauncherViewModel", "No calendar app found in app list, using system intent")
            val calendarIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_CALENDAR)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(calendarIntent)
        } catch (e: Exception) {
            android.util.Log.e("LauncherViewModel", "Failed to launch calendar app via system intent: ${e.message}", e)
            
            // Last resort: try to open calendar via VIEW intent
            try {
                android.util.Log.d("LauncherViewModel", "Trying VIEW intent for calendar as last resort")
                val viewCalendarIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("content://com.android.calendar/time/")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewCalendarIntent)
            } catch (e2: Exception) {
                android.util.Log.e("LauncherViewModel", "All calendar launch attempts failed: ${e2.message}", e2)
                // We've tried everything, nothing more we can do
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

    // New method to launch the dialer app
    fun launchDialerApp() {
        val context = getApplication<Application>()
        
        // First try to find the dialer app by name or package
        val dialerApp = apps.value.firstOrNull { app -> 
            app.appName.equals("Phone", ignoreCase = true) || 
            app.appName.equals("Dialer", ignoreCase = true) ||
            app.packageName.contains("dialer", ignoreCase = true) ||
            app.packageName.contains("phone", ignoreCase = true)
        }
        
        if (dialerApp != null) {
            // Launch the dialer app if found
            launchApp(dialerApp.packageName, dialerApp.launchIntent)
            return
        }
        
        try {
            // Fall back to the system dialer intent if custom app not found
            val dialerIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialerIntent)
        } catch (e: Exception) {
            // Log or handle the exception
        }
    }
    
    /**
     * Launch a specific calendar event by its ID
     * @param eventId The event ID
     * @param calendarId The calendar ID
     */
    fun openCalendarEvent(eventId: Long?, calendarId: Long?) {
        val context = getApplication<Application>()
        
        android.util.Log.d("LauncherViewModel", "Opening calendar event: eventId=$eventId, calendarId=$calendarId")
        
        // If we don't have a valid event ID, fall back to opening the calendar app
        if (eventId == null) {
            android.util.Log.d("LauncherViewModel", "No eventId provided, falling back to calendar app")
            launchCalendarApp()
            return
        }
        
        try {
            // Create an intent to view the specific event
            val viewEventIntent = Intent(Intent.ACTION_VIEW).apply {
                // Format: content://com.android.calendar/events/EVENT_ID
                val uri = android.net.Uri.parse("content://com.android.calendar/events/$eventId")
                data = uri
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            android.util.Log.d("LauncherViewModel", "Launching intent with URI: content://com.android.calendar/events/$eventId")
            context.startActivity(viewEventIntent)
        } catch (e: Exception) {
            // If viewing the specific event fails, fall back to the calendar app
            android.util.Log.e("LauncherViewModel", "Failed to open event: ${e.message}", e)
            launchCalendarApp()
        }
    }

    /**
     * Reset the open counter for a specific app
     * @param packageName The package name of the app
     */
    fun resetAppCounter(packageName: String) {
        appUsagePrefs.edit().remove(packageName).apply()
        // Refresh the app list to reflect the new sorting
        loadApps()
    }

    /**
     * Open the system settings page for a specific app
     * @param packageName The package name of the app
     */
    fun openAppSettings(packageName: String) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("LauncherViewModel", "Failed to open app settings: ${e.message}", e)
        }
    }

    /**
     * Uninstall an app by launching the system uninstall dialog
     * @param packageName The package name of the app
     */
    fun uninstallApp(packageName: String) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = android.net.Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("LauncherViewModel", "Failed to launch uninstall dialog: ${e.message}", e)
        }
    }
} 