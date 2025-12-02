package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.EntropysDefianceApplication
import com.example.entropysdefiance.data.DatabaseMigrationHelper
import com.example.entropysdefiance.data.TaskData
import com.example.entropysdefiance.data.TaskFileRepository
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import com.example.entropysdefiance.utils.calculateTimeHorizonFromDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val TASK_VM_TAG = "TaskViewModel"
private const val POLLING_INTERVAL_MS = 2000L // Check every 2 seconds

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    
    // File repository for JSON storage
    private val fileRepository = TaskFileRepository(application)
    
    // Settings repository to get file path
    private val settingsRepository = (application as EntropysDefianceApplication).settingsRepository
    
    // In-memory task list with reactive state flow
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    // Track last file modification time for change detection
    private var lastFileHash = 0
    
    // Track if we're currently writing to prevent reload loops
    private var isWriting = false
    
    // SharedPreferences to track last update date
    private val prefs: SharedPreferences = application.getSharedPreferences(
        "task_horizon_updates", Context.MODE_PRIVATE
    )
    private val LAST_UPDATE_DATE_KEY = "last_deadline_update_date"
    
    // Internal storage for full task data (includes metadata like autoId, createdAt, isCompleted, daysNeeded)
    private val _taskDataList = MutableStateFlow<List<TaskData>>(emptyList())
    
    init {
        // Initialize by loading tasks from file
        viewModelScope.launch {
            // Check for old database and migrate if needed
            checkAndMigrateFromOldDatabase()
            
            loadTasksFromFile()
            
            // Start periodic file polling
            startFilePolling()
        }
    }
    
    /**
     * Check if old Room database exists and migrate automatically
     */
    private suspend fun checkAndMigrateFromOldDatabase() {
        try {
            if (DatabaseMigrationHelper.hasOldDatabase(getApplication())) {
                Log.d(TASK_VM_TAG, "Old database found, starting automatic migration")
                
                val migratedTasks = DatabaseMigrationHelper.migrateFromOldDatabase(getApplication())
                
                if (migratedTasks != null && migratedTasks.isNotEmpty()) {
                    Log.d(TASK_VM_TAG, "Migrated ${migratedTasks.size} tasks from old database")
                    
                    // Get current file path
                    val settings = settingsRepository.settingsFlow.first()
                    val filePath = settings.taskFilePath
                    
                    if (filePath.isNotEmpty()) {
                        // Write migrated tasks to file
                        val success = fileRepository.writeTasksToFile(migratedTasks, filePath)
                        
                        if (success) {
                            Log.d(TASK_VM_TAG, "Successfully wrote migrated tasks to file")
                            // Delete old database after successful migration
                            DatabaseMigrationHelper.deleteOldDatabase(getApplication())
                        } else {
                            Log.e(TASK_VM_TAG, "Failed to write migrated tasks to file")
                        }
                    } else {
                        Log.w(TASK_VM_TAG, "No file path configured, cannot complete migration")
                    }
                } else {
                    Log.d(TASK_VM_TAG, "No tasks found in old database or migration failed")
                }
            }
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error during database migration: ${e.message}", e)
        }
    }
    
    /**
     * Start polling the file for external changes
     */
    private fun startFilePolling() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(POLLING_INTERVAL_MS)
                checkForExternalChanges()
            }
        }
    }
    
    /**
     * Check if the file has changed externally
     */
    private suspend fun checkForExternalChanges() {
        if (isWriting) return // Don't check while we're writing
        
        try {
            val settings = settingsRepository.settingsFlow.first()
            val filePath = settings.taskFilePath
            
            if (filePath.isEmpty()) return
            
            // Read the file and compute a simple hash
            val tasksFromFile = fileRepository.readTasksFromFile(filePath)
            val currentHash = computeTaskListHash(tasksFromFile)
            
            // If hash changed, reload tasks
            if (currentHash != lastFileHash && lastFileHash != 0) {
                Log.d(TASK_VM_TAG, "External file change detected, reloading tasks")
                loadTasksFromFile()
            }
            
            lastFileHash = currentHash
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error checking for external changes: ${e.message}", e)
        }
    }
    
    /**
     * Compute a simple hash of the task list for change detection
     */
    private fun computeTaskListHash(tasks: List<TaskData>): Int {
        return tasks.sortedBy { it.id }.joinToString { "${it.id}:${it.title}:${it.isCompleted}" }.hashCode()
    }
    
    /**
     * Load tasks from JSON file
     */
    private suspend fun loadTasksFromFile() {
        try {
            val settings = settingsRepository.settingsFlow.first()
            val filePath = settings.taskFilePath
            
            if (filePath.isEmpty()) {
                Log.d(TASK_VM_TAG, "No file path configured, using empty task list")
                _taskDataList.value = emptyList()
                _tasks.value = emptyList()
                return
            }
            
            Log.d(TASK_VM_TAG, "Loading tasks from file: $filePath")
            
            val tasksFromFile = fileRepository.readTasksFromFile(filePath)
            _taskDataList.value = tasksFromFile
            
            // Convert to Task models and filter out completed tasks, then sort
            val pendingTasks = tasksFromFile
                .filter { !it.isCompleted }
                .map { it.toTask() }
                .sortedWith(
                    compareBy<Task> { !it.isDeadlineTask } // Deadline tasks first
                        .thenBy { it.deadline } // Sort by deadline ascending
                        .thenBy { 
                            when (it.timeHorizon) {
                                TimeHorizon.Today -> 0
                                TimeHorizon.ThisWeek -> 1
                                TimeHorizon.ThisMonth -> 2
                                TimeHorizon.ThisYear -> 3
                                TimeHorizon.ThisLife -> 4
                            }
                        }
                )
            
            _tasks.value = pendingTasks
            lastFileHash = computeTaskListHash(tasksFromFile)
            
            Log.d(TASK_VM_TAG, "Loaded ${tasksFromFile.size} total tasks, ${pendingTasks.size} pending")
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error loading tasks from file: ${e.message}", e)
            _taskDataList.value = emptyList()
            _tasks.value = emptyList()
        }
    }
    
    /**
     * Write current tasks to JSON file
     */
    private suspend fun writeTasksToFile() {
        try {
            isWriting = true
            
            val settings = settingsRepository.settingsFlow.first()
            val filePath = settings.taskFilePath
            
            if (filePath.isEmpty()) {
                Log.d(TASK_VM_TAG, "No file path configured, skipping write")
                return
            }
            
            val tasks = _taskDataList.value
            Log.d(TASK_VM_TAG, "Writing ${tasks.size} tasks to file: $filePath")
            
            fileRepository.writeTasksToFile(tasks, filePath)
            lastFileHash = computeTaskListHash(tasks)
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error writing tasks to file: ${e.message}", e)
        } finally {
            isWriting = false
        }
    }

    fun addTask(title: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            // Get the next auto-incrementing ID
            val lastAutoId = _taskDataList.value.maxOfOrNull { it.autoId ?: 0L } ?: 0L
            val nextAutoId = lastAutoId + 1L
            
            val taskData = TaskData(
                id = UUID.randomUUID().toString(),
                title = title,
                timeHorizon = timeHorizon.name,
                autoId = nextAutoId,
                createdAt = LocalDateTime.now(),
                isCompleted = false,
                isDeadlineTask = false,
                deadline = null,
                daysNeeded = null,
                doDate = null
            )
            
            _taskDataList.value = _taskDataList.value + taskData
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }
    
    fun addDeadlineTask(title: String, deadline: LocalDate, daysNeeded: Int) {
        viewModelScope.launch {
            // Calculate do-date: deadline - daysNeeded
            val doDate = deadline.minusDays(daysNeeded.toLong())
            
            // Calculate time horizon based on do-date
            val timeHorizon = calculateTimeHorizonFromDate(doDate)
            
            // Get the next auto-incrementing ID
            val lastAutoId = _taskDataList.value.maxOfOrNull { it.autoId ?: 0L } ?: 0L
            val nextAutoId = lastAutoId + 1L
            
            val taskData = TaskData(
                id = UUID.randomUUID().toString(),
                title = title,
                timeHorizon = timeHorizon.name,
                autoId = nextAutoId,
                createdAt = LocalDateTime.now(),
                isCompleted = false,
                isDeadlineTask = true,
                deadline = deadline,
                daysNeeded = daysNeeded,
                doDate = doDate
            )
            
            _taskDataList.value = _taskDataList.value + taskData
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            _taskDataList.value = _taskDataList.value.map { task ->
                if (task.id == taskId) {
                    task.copy(isCompleted = true)
                } else {
                    task
                }
            }
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _taskDataList.value = _taskDataList.value.filter { it.id != taskId }
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }

    fun moveTask(taskId: String, newTimeHorizon: TimeHorizon) {
        viewModelScope.launch {
            _taskDataList.value = _taskDataList.value.map { task ->
                if (task.id == taskId && !task.isDeadlineTask) {
                    task.copy(timeHorizon = newTimeHorizon.name)
                } else {
                    task
                }
            }
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }
    
    fun updateTask(taskId: String, newTitle: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            _taskDataList.value = _taskDataList.value.map { task ->
                if (task.id == taskId) {
                    task.copy(title = newTitle, timeHorizon = timeHorizon.name)
                } else {
                    task
                }
            }
            writeTasksToFile()
            loadTasksFromFile() // Reload to update UI
        }
    }
    
    /**
     * Check if deadline task horizons need to be updated (if it's a new day since last update).
     * Updates the horizons if needed.
     */
    fun checkAndUpdateDeadlineTaskHorizons() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val lastUpdateDateString = prefs.getString(LAST_UPDATE_DATE_KEY, null)
            
            // Check if we need to update (new day or never updated)
            val needsUpdate = if (lastUpdateDateString != null) {
                val lastUpdateDate = LocalDate.parse(lastUpdateDateString)
                today.isAfter(lastUpdateDate)
            } else {
                true // Never updated, so update now
            }
            
            if (needsUpdate) {
                updateDeadlineTaskHorizons()
                // Store today's date as the last update date
                prefs.edit().putString(LAST_UPDATE_DATE_KEY, today.toString()).apply()
            }
        }
    }
    
    /**
     * Update the time horizons for all deadline tasks based on their do-dates.
     * This should be called daily to keep tasks in the correct horizon.
     */
    fun updateDeadlineTaskHorizons() {
        viewModelScope.launch {
            var updated = false
            
            _taskDataList.value = _taskDataList.value.map { task ->
                if (task.isDeadlineTask && task.doDate != null) {
                    val newHorizon = calculateTimeHorizonFromDate(task.doDate)
                    if (newHorizon.name != task.timeHorizon) {
                        updated = true
                        task.copy(timeHorizon = newHorizon.name)
                    } else {
                        task
                    }
                } else {
                    task
                }
            }
            
            if (updated) {
                writeTasksToFile()
                loadTasksFromFile() // Reload to update UI
            }
        }
    }
    
    suspend fun getTotalTaskCount(): Int {
        return _taskDataList.value.size
    }
    
    suspend fun getCompletedTaskCount(): Int {
        return _taskDataList.value.count { it.isCompleted }
    }
    
    suspend fun getPendingTaskCount(): Int {
        return _taskDataList.value.count { !it.isCompleted }
    }
    
    suspend fun getOldestTaskDate(): LocalDateTime? {
        return _taskDataList.value.mapNotNull { it.createdAt }.minOrNull()
    }
    
    /**
     * Trigger manual sync from file
     */
    fun syncFromFile() {
        viewModelScope.launch {
            loadTasksFromFile()
        }
    }
    
    /**
     * Trigger manual sync to file
     */
    fun syncToFile() {
        viewModelScope.launch {
            writeTasksToFile()
        }
    }

    /**
     * Check for external file changes (called from MainActivity)
     */
    fun checkForExternalFileChanges() {
        viewModelScope.launch {
            checkForExternalChanges()
        }
    }
    
    /**
     * Update file path and reload tasks
     */
    fun updateFilePath(newPath: String) {
        viewModelScope.launch {
            settingsRepository.updateTaskFilePath(newPath)
            loadTasksFromFile()
        }
    }
    
    /**
     * Import tasks from CSV file and merge with existing tasks
     */
    suspend fun importTasksFromCsv(tasks: List<TaskData>): Result<Int> {
        return try {
            // Merge with existing tasks (avoid duplicates by ID)
            val existingIds = _taskDataList.value.map { it.id }.toSet()
            val newTasks = tasks.filter { it.id !in existingIds }
            
            if (newTasks.isEmpty()) {
                Result.success(0)
            } else {
                _taskDataList.value = _taskDataList.value + newTasks
                writeTasksToFile()
                loadTasksFromFile()
                Result.success(newTasks.size)
            }
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error importing tasks from CSV: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Replace all tasks with imported tasks (for full restore)
     */
    suspend fun replaceAllTasks(tasks: List<TaskData>): Result<Int> {
        return try {
            _taskDataList.value = tasks
            writeTasksToFile()
            loadTasksFromFile()
            Result.success(tasks.size)
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error replacing tasks: ${e.message}", e)
            Result.failure(e)
        }
    }
}
