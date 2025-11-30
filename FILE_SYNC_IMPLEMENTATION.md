# File Sync Implementation Summary

## Overview
Implemented a hybrid approach that keeps Room database for fast queries while syncing tasks to/from a JSON file for cross-device editing (e.g., with Syncthing).

## Architecture

### Hybrid Sync Model (Option 2)
- **Room Database**: Primary storage for app operations (fast queries, reactive UI)
- **JSON File**: External sync point for laptop editing and Syncthing
- **File Observer**: Detects external changes and reloads tasks
- **Automatic Sync**: Tasks are synced to file on every modification

## Files Created

### 1. `TaskJson.kt` - Serialization Models
- `LocalDateTimeSerializer`: Custom serializer for LocalDateTime
- `LocalDateSerializer`: Custom serializer for LocalDate  
- `TaskJson`: JSON representation of TaskEntity
- `TasksFileJson`: Root JSON structure with version and task list

### 2. `TaskFileRepository.kt` - File Operations
- `readTasksFromFile()`: Load tasks from JSON file
- `writeTasksToFile()`: Save tasks to JSON file (atomic write with temp file)
- `getTaskFile()`: Get file based on custom path or default
- `createEmptyFileIfNeeded()`: Initialize empty JSON file
- Default location: `/storage/emulated/0/Documents/EntropysDefiance/tasks.json`

### 3. `TaskFileObserver.kt` - External Change Detection
- Watches the tasks.json file for external modifications
- Triggers reload when file is modified by external tools
- Debounces changes to prevent loops

### 4. `FileSyncSettingsScreen.kt` - UI Component
- Configure custom file path for tasks.json
- "Browse for File" button using Android's file manager
- Manual sync buttons (Load from File / Save to File)
- Info card explaining file sync feature
- Shows current file location

## Files Modified

### 1. `AppSettings.kt`
- Added `taskFilePath: String` field for storing custom file location

### 2. `SettingsRepository.kt`
- Added `TASK_FILE_PATH` preference key
- Added `updateTaskFilePath()` method
- Integrated into settings flow and update methods

### 3. `TaskViewModel.kt`
- Added `fileRepository: TaskFileRepository`
- Added `fileObserver: TaskFileObserver`
- Added `isSyncing` flag to prevent infinite loops
- Added `initializeFileSync()`: Load from file on first launch
- Added `loadTasksFromFile()`: Sync file → Room
- Added `writeTasksToFile()`: Sync Room → file
- Added `setupFileObserver()`: Watch for external changes
- Added `syncFromFile()` and `syncToFile()`: Manual sync methods
- Added `updateFilePath()`: Change file location and reinitialize
- Modified all task operations (add, update, delete, complete, move) to sync to file
- Added `onCleared()` to stop file observer

### 4. `SettingsViewModel.kt`
- Added `showFileSyncSettings` state flow
- Added `showFileSyncSettings()` and `hideFileSyncSettings()` navigation methods

### 5. `SettingsDialog.kt`
- Added `onFileSyncSettingsClick` parameter
- Added File Sync section button with Sync icon

### 6. `MainActivity.kt`
- Imported `FileSyncSettingsScreen`
- Added `showFileSyncSettings` state collection
- Added `storagePermissionLauncher` for requesting storage permissions
- Added storage permission check in LaunchedEffect
- Added FileSyncSettingsScreen rendering with callbacks to TaskViewModel

### 7. `AndroidManifest.xml`
- Added `WRITE_EXTERNAL_STORAGE` permission (for Android ≤ 32)
- Added `MANAGE_EXTERNAL_STORAGE` permission (for Android 13+)

## JSON File Format

```json
{
  "version": 1,
  "tasks": [
    {
      "id": "uuid-string",
      "title": "Task title",
      "timeHorizon": "Today",
      "autoId": 123,
      "createdAt": "2025-11-30T10:30:00",
      "isCompleted": false,
      "isDeadlineTask": true,
      "deadline": "2025-12-05",
      "daysNeeded": 3,
      "doDate": "2025-12-02"
    }
  ]
}
```

## How It Works

### On App Start
1. TaskViewModel initializes file sync
2. Loads tasks from JSON file (if exists) into Room database
3. If file is empty but Room has tasks, writes them to file
4. Sets up FileObserver to watch for external changes

### On Task Modification
1. Task is modified in Room database (add/update/delete/complete)
2. UI updates reactively via Flow
3. Tasks are written to JSON file
4. FileObserver ignores self-writes (isSyncing flag)

### On External File Change
1. FileObserver detects change (e.g., Syncthing sync or laptop edit)
2. Triggers `loadTasksFromFile()`
3. Room database is cleared and repopulated from file
4. UI updates reactively

### Manual Sync
- **Load from File**: Forces reload from JSON → Room
- **Save to File**: Forces write from Room → JSON
- Useful for debugging or resolving conflicts

## File Path Configuration

### Default Location
`/storage/emulated/0/Documents/EntropysDefiance/tasks.json`

### Custom Location
Users can set a custom path in Settings → File Sync
- Useful for Syncthing folders
- Example: `/storage/emulated/0/Syncthing/EntropysDefiance/tasks.json`

## Conflict Resolution

### Current Behavior
- Last write wins (file overwrites Room or vice versa)
- No merge logic implemented

### Future Enhancements
- Conflict detection (check timestamps)
- Merge strategies for concurrent edits
- Backup of conflicting versions
- Manual conflict resolution UI

## Laptop Editing

### Simple Editing
- Use any text editor to edit `tasks.json`
- Format is human-readable JSON
- Syncthing will sync changes to phone
- App will detect and reload automatically

### TUI Options
1. **jq**: Command-line JSON querying
   ```bash
   jq '.tasks[] | select(.isCompleted == false)' tasks.json
   ```

2. **Python script**: Simple task manager
   ```python
   import json
   with open('tasks.json', 'r') as f:
       data = json.load(f)
   # Manipulate data
   with open('tasks.json', 'w') as f:
       json.dump(data, f, indent=2)
   ```

3. **Full TUI**: Use Python's `curses` or `rich` library

## Testing Notes

The implementation is complete but could not be fully tested due to Gradle compatibility issues with Java 25. The code structure and logic are sound based on:

1. All necessary imports are in place
2. File structure is correct
3. Method signatures match
4. No obvious syntax errors in manual review

## Build Issues Encountered

- Gradle 8.13 has compatibility issues with Java 25
- Error: `java.lang.IllegalArgumentException: 25.0.1`
- **Solution**: Use Java 17 - `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug`
- Build successful with Java 17!

## Permissions Required

- `READ_EXTERNAL_STORAGE`: Read files from external storage
- `WRITE_EXTERNAL_STORAGE`: Write files to external storage (Android ≤ 32)
- `MANAGE_EXTERNAL_STORAGE`: Full file access (Android 13+)

Users will be prompted for storage permissions on first launch.

## Build Instructions

To build the app with file sync support:

```bash
# Use Java 17 (required for Gradle 8.13)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## Using the Laptop Task Manager

A Python script is provided for easy task management on your laptop:

```bash
# Interactive mode
python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json

# Quick commands
python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json list
python laptop_task_manager.py ~/Syncthing/EntropysDefiance/tasks.json add "New task title"
```

## Next Steps

1. **Test on device**: Install APK and verify file sync works
2. **Configure Syncthing**: Set up folder sync between phone and laptop
3. **Test bidirectional sync**: Edit on both devices and verify
4. **Test conflict scenarios**: Edit file while app is open
5. **Enhance laptop TUI**: Add deadline task support, filtering, etc.
6. **Add conflict resolution**: Implement merge strategies for concurrent edits
