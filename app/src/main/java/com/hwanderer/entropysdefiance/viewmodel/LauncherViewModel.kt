// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.SharedPreferences
import android.os.Process
import android.provider.CalendarContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.EntropysDefianceApplication
import com.hwanderer.entropysdefiance.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    // Everything the system reports, before the user's hide/rename choices are applied.
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val settingsRepository =
        (application as EntropysDefianceApplication).settingsRepository

    /**
     * The drawer's view of the world: hidden entries removed, renames applied.
     *
     * Derived from the raw list rather than re-enumerated, so hiding or renaming shows up
     * immediately without paying for another `queryIntentActivities` sweep.
     */
    val apps: StateFlow<List<AppInfo>> =
        combine(_apps, settingsRepository.settingsFlow) { all, settings ->
            all.asSequence()
                .filterNot { it.usageKey in settings.hiddenApps }
                .map { app -> app.withRename(settings.appRenames[app.usageKey]) }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Hidden entries, for the "Hidden apps" settings screen. */
    val hiddenApps: StateFlow<List<AppInfo>> =
        combine(_apps, settingsRepository.settingsFlow) { all, settings ->
            all.filter { it.usageKey in settings.hiddenApps }
                // Renamed here too — an app you hid is one you know by the name you gave it.
                .map { app -> app.withRename(settings.appRenames[app.usageKey]) }
                .sortedBy { it.appName.lowercase() }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun AppInfo.withRename(customName: String?): AppInfo =
        if (customName.isNullOrBlank()) this
        else copy(appName = customName, originalName = appName)

    fun setAppHidden(app: AppInfo, hidden: Boolean) {
        viewModelScope.launch { settingsRepository.setAppHidden(app.usageKey, hidden) }
    }

    /** A blank name clears the override and restores the system label. */
    fun setAppRename(app: AppInfo, customName: String?) {
        viewModelScope.launch { settingsRepository.setAppRename(app.usageKey, customName) }
    }
    
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

                // Drop any exact-duplicate launcher activities (same package + activity), then
                // sort by usage count (most used first). distinctBy on the unique listKey is a
                // belt-and-braces guard so the drawer never carries colliding list keys.
                (appList + shortcutList)
                    .distinctBy { it.listKey }
                    .sortedByDescending { app ->
                        appUsagePrefs.getInt(app.usageKey, 0)
                    }
            }

            _apps.value = sortedAppList
        }
    }

    private fun loadPinnedShortcuts(context: Context): List<AppInfo> {
        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps ?: return emptyList()
            val hasPermission = try {
                launcherApps.hasShortcutHostPermission()
            } catch (e: Exception) {
                false
            }
            if (!hasPermission) {
                return emptyList()
            }
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
        } catch (e: SecurityException) {
            android.util.Log.d("LauncherViewModel", "Cannot access shortcuts (not default launcher or permission denied): ${e.message}")
            emptyList()
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
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val hasPermission = try {
                launcherApps?.hasShortcutHostPermission() == true
            } catch (e: Exception) {
                false
            }
            if (!hasPermission) {
                android.util.Log.w("LauncherViewModel", "Cannot launch shortcut: app lacks shortcut host permission")
                return
            }
            try {
                launcherApps?.startShortcut(packageName, shortcutId, null, null, Process.myUserHandle())
                val key = "shortcut:$packageName:$shortcutId"
                appUsagePrefs.edit().putInt(key, appUsagePrefs.getInt(key, 0) + 1).apply()
            } catch (e: SecurityException) {
                android.util.Log.w("LauncherViewModel", "SecurityException launching shortcut: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.e("LauncherViewModel", "Failed to launch shortcut: ${e.message}", e)
            }
            loadApps()
            return
        }

        // Launch the app. Guard startActivity: the activity may be stale/unlaunchable (e.g. the
        // auto-launch-on-single-match path in AppDrawer can fire on an entry that no longer
        // resolves), and an uncaught exception here would crash the launcher process.
        val intent = Intent().apply {
            setClassName(packageName, launchIntent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
            // Increment usage count only after a successful launch.
            val currentCount = appUsagePrefs.getInt(packageName, 0)
            appUsagePrefs.edit().putInt(packageName, currentCount + 1).apply()
        } catch (e: Exception) {
            android.util.Log.e("LauncherViewModel", "Failed to launch app $packageName: ${e.message}", e)
        }

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
     * @param eventId The event ID (master series ID for recurring events)
     * @param calendarId The calendar ID
     * @param beginMillis The tapped occurrence's start time (epoch millis), used to pin a recurring
     *                    series to the right instance instead of opening its first occurrence
     * @param endMillis The tapped occurrence's end time (epoch millis)
     */
    fun openCalendarEvent(
        eventId: Long?,
        calendarId: Long?,
        beginMillis: Long? = null,
        endMillis: Long? = null
    ) {
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
                // Pin to the tapped occurrence of a recurring series. Without these extras the
                // calendar opens the series at its first occurrence regardless of which date was
                // tapped, since eventId is the same for every occurrence.
                if (beginMillis != null) {
                    putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
                }
                if (endMillis != null) {
                    putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                }
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
     * Uninstall an app by launching the system uninstall dialog. The user confirms in the
     * system UI; this only opens it. Requires the REQUEST_DELETE_PACKAGES permission declared in
     * the manifest — without it the system rejects ACTION_DELETE and the dialog never appears.
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