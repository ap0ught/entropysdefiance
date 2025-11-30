package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.EntropysDefianceApplication
import com.example.entropysdefiance.data.TaskDatabase
import com.example.entropysdefiance.data.TaskEntity
import com.example.entropysdefiance.data.TaskFileRepository
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import com.example.entropysdefiance.utils.TaskFileObserver
import com.example.entropysdefiance.utils.calculateTimeHorizonFromDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private const val TASK_VM_TAG = "TaskViewModel"

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TaskDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    
    // File repository for JSON sync
    private val fileRepository = TaskFileRepository(application)
    
    // Settings repository to get file path
    private val settingsRepository = (application as EntropysDefianceApplication).settingsRepository
    
    // File observer for detecting external changes
    private var fileObserver: TaskFileObserver? = null
    
    // Track if we're currently syncing to prevent infinite loops
    private var isSyncing = false
    
    // SharedPreferences to track last update date
    private val prefs: SharedPreferences = application.getSharedPreferences(
        "task_horizon_updates", Context.MODE_PRIVATE
    )
    private val LAST_UPDATE_DATE_KEY = "last_deadline_update_date"
    
    init {
        // Initialize file sync
        viewModelScope.launch {
            initializeFileSync()
        }
    }
    
    val tasks = taskDao.getPendingTasks().map { entities ->
        // Convert to tasks and sort: deadline tasks first (sorted by deadline ascending - earliest first), then regular tasks by horizon priority
        entities.map { it.toTask() }
            .sortedWith(
                compareBy<Task> { !it.isDeadlineTask } // Deadline tasks first
                    .thenBy { 
                        // For deadline tasks: sort by deadline ascending (earliest deadlines first)
                        // For non-deadline tasks: this will be null, so they'll be sorted by the next comparator
                        it.deadline
                    }
                    .thenBy { 
                        // For non-deadline tasks: sort by horizon priority (Today -> ThisWeek -> ThisMonth -> ThisYear -> ThisLife)
                        if (!it.isDeadlineTask) {
                            when (it.timeHorizon) {
                                TimeHorizon.Today -> 0
                                TimeHorizon.ThisWeek -> 1
                                TimeHorizon.ThisMonth -> 2
                                TimeHorizon.ThisYear -> 3
                                TimeHorizon.ThisLife -> 4
                            }
                        } else {
                            // For deadline tasks that have the same deadline, maintain horizon priority
                            when (it.timeHorizon) {
                                TimeHorizon.Today -> 0
                                TimeHorizon.ThisWeek -> 1
                                TimeHorizon.ThisMonth -> 2
                                TimeHorizon.ThisYear -> 3
                                TimeHorizon.ThisLife -> 4
                            }
                        }
                    }
            )
    }

    fun addTask(title: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            // Get the next auto-incrementing ID
            val lastTask = taskDao.getLastTask()
            val nextAutoId = (lastTask?.autoId ?: 0L) + 1L
            
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                timeHorizon = timeHorizon,
                autoId = nextAutoId,
                createdAt = java.time.LocalDateTime.now(),
                isCompleted = false,
                isDeadlineTask = false,
                deadline = null,
                daysNeeded = null,
                doDate = null
            )
            taskDao.insertTask(task)
            
            // Sync to file
            val settings = settingsRepository.settingsFlow.first()
            writeTasksToFile(settings.taskFilePath)
        }
    }
    
    fun addDeadlineTask(title: String, deadline: LocalDate, daysNeeded: Int) {
        viewModelScope.launch {
            // Calculate do-date: deadline - daysNeeded
            val doDate = deadline.minusDays(daysNeeded.toLong())
            
            // Calculate time horizon based on do-date
            val timeHorizon = calculateTimeHorizonFromDate(doDate)
            
            // Get the next auto-incrementing ID
            val lastTask = taskDao.getLastTask()
            val nextAutoId = (lastTask?.autoId ?: 0L) + 1L
            
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                timeHorizon = timeHorizon,
                autoId = nextAutoId,
                createdAt = java.time.LocalDateTime.now(),
                isCompleted = false,
                isDeadlineTask = true,
                deadline = deadline,
                daysNeeded = daysNeeded,
                doDate = doDate
            )
            taskDao.insertTask(task)
            
            // Sync to file
            val settings = settingsRepository.settingsFlow.first()
            writeTasksToFile(settings.taskFilePath)
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskDao.markTaskAsCompleted(taskId)
            
            // Sync to file
            val settings = settingsRepository.settingsFlow.first()
            writeTasksToFile(settings.taskFilePath)
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskDao.deleteTask(taskId)
            
            // Sync to file
            val settings = settingsRepository.settingsFlow.first()
            writeTasksToFile(settings.taskFilePath)
        }
    }

    fun moveTask(taskId: String, newTimeHorizon: TimeHorizon) {
        viewModelScope.launch {
            taskDao.getTaskById(taskId)?.let { taskEntity ->
                // Prevent moving deadline tasks
                if (taskEntity.isDeadlineTask) {
                    return@launch
                }
                
                val updatedTask = TaskEntity(
                    autoId = taskEntity.autoId,
                    id = taskEntity.id,
                    title = taskEntity.title,
                    timeHorizon = newTimeHorizon,
                    createdAt = taskEntity.createdAt ?: java.time.LocalDateTime.now(),
                    isCompleted = taskEntity.isCompleted,
                    isDeadlineTask = taskEntity.isDeadlineTask,
                    deadline = taskEntity.deadline,
                    daysNeeded = taskEntity.daysNeeded,
                    doDate = taskEntity.doDate
                )
                taskDao.insertTask(updatedTask)
                
                // Sync to file
                val settings = settingsRepository.settingsFlow.first()
                writeTasksToFile(settings.taskFilePath)
            }
        }
    }
    
    fun updateTask(taskId: String, newTitle: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            taskDao.getTaskById(taskId)?.let { taskEntity ->
                val updatedTask = TaskEntity(
                    autoId = taskEntity.autoId,
                    id = taskId,
                    title = newTitle,
                    timeHorizon = timeHorizon,
                    createdAt = taskEntity.createdAt ?: java.time.LocalDateTime.now(),
                    isCompleted = taskEntity.isCompleted,
                    isDeadlineTask = taskEntity.isDeadlineTask,
                    deadline = taskEntity.deadline,
                    daysNeeded = taskEntity.daysNeeded,
                    doDate = taskEntity.doDate
                )
                taskDao.insertTask(updatedTask)
                
                // Sync to file
                val settings = settingsRepository.settingsFlow.first()
                writeTasksToFile(settings.taskFilePath)
            }
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
            val entities = taskDao.getAllTasks().first()
            var hasChanges = false
            entities.forEach { taskEntity ->
                if (taskEntity.isDeadlineTask && taskEntity.doDate != null) {
                    val task = taskEntity.toTask()
                    val doDate = taskEntity.doDate // Safe because we checked null above
                    val newHorizon = calculateTimeHorizonFromDate(doDate)
                    if (newHorizon != task.timeHorizon) {
                        val updatedTask = TaskEntity(
                            autoId = taskEntity.autoId,
                            id = taskEntity.id,
                            title = taskEntity.title,
                            timeHorizon = newHorizon,
                            createdAt = taskEntity.createdAt ?: java.time.LocalDateTime.now(),
                            isCompleted = taskEntity.isCompleted,
                            isDeadlineTask = true,
                            deadline = taskEntity.deadline,
                            daysNeeded = taskEntity.daysNeeded,
                            doDate = taskEntity.doDate
                        )
                        taskDao.insertTask(updatedTask)
                        hasChanges = true
                    }
                }
            }
            
            // Sync to file if there were changes
            if (hasChanges) {
                val settings = settingsRepository.settingsFlow.first()
                writeTasksToFile(settings.taskFilePath)
            }
        }
    }
    
    suspend fun getTotalTaskCount(): Int {
        return taskDao.getTotalTaskCount()
    }
    
    suspend fun getCompletedTaskCount(): Int {
        return taskDao.getCompletedTaskCount()
    }
    
    suspend fun getPendingTaskCount(): Int {
        return taskDao.getPendingTaskCount()
    }
    
    suspend fun getOldestTaskDate(): java.time.LocalDateTime? {
        return taskDao.getOldestTaskDate()
    }
    
    /**
     * Initialize file sync: load from file and set up file observer
     */
    private suspend fun initializeFileSync() {
        try {
            val settings = settingsRepository.settingsFlow.first()
            val filePath = settings.taskFilePath
            
            Log.d(TASK_VM_TAG, "Initializing file sync with path: $filePath")
            
            // Load tasks from file to Room on first launch
            loadTasksFromFile(filePath)
            
            // Set up file observer
            setupFileObserver(filePath)
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error initializing file sync: ${e.message}", e)
        }
    }
    
    /**
     * Load tasks from JSON file into Room database
     */
    private suspend fun loadTasksFromFile(filePath: String) {
        if (isSyncing) return
        
        try {
            isSyncing = true
            Log.d(TASK_VM_TAG, "Loading tasks from file: $filePath")
            
            val tasksFromFile = fileRepository.readTasksFromFile(filePath)
            
            if (tasksFromFile.isEmpty()) {
                Log.d(TASK_VM_TAG, "No tasks in file, syncing current Room tasks to file")
                // If file is empty, write current Room tasks to it
                val currentTasks = taskDao.getAllTasks().first()
                if (currentTasks.isNotEmpty()) {
                    fileRepository.writeTasksToFile(currentTasks, filePath)
                }
            } else {
                Log.d(TASK_VM_TAG, "Loaded ${tasksFromFile.size} tasks from file, updating Room")
                // Clear Room and insert tasks from file
                withContext(Dispatchers.IO) {
                    // Delete all current tasks
                    val currentTasks = taskDao.getAllTasks().first()
                    currentTasks.forEach { taskDao.deleteTask(it.id) }
                    
                    // Insert tasks from file
                    tasksFromFile.forEach { taskDao.insertTask(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error loading tasks from file: ${e.message}", e)
        } finally {
            isSyncing = false
        }
    }
    
    /**
     * Write current Room tasks to JSON file
     */
    private suspend fun writeTasksToFile(filePath: String) {
        if (isSyncing) return
        
        try {
            isSyncing = true
            val tasks = taskDao.getAllTasks().first()
            Log.d(TASK_VM_TAG, "Writing ${tasks.size} tasks to file: $filePath")
            fileRepository.writeTasksToFile(tasks, filePath)
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error writing tasks to file: ${e.message}", e)
        } finally {
            isSyncing = false
        }
    }
    
    /**
     * Set up file observer to watch for external changes
     */
    private fun setupFileObserver(filePath: String) {
        try {
            // Stop existing observer
            fileObserver?.stopWatchingFile()
            
            val file = fileRepository.getTaskFile(filePath)
            if (!file.exists()) {
                // Create empty file if it doesn't exist
                viewModelScope.launch {
                    fileRepository.createEmptyFileIfNeeded(filePath)
                }
            }
            
            fileObserver = TaskFileObserver(file) {
                // File changed externally, reload tasks
                viewModelScope.launch {
                    Log.d(TASK_VM_TAG, "External file change detected, reloading tasks")
                    loadTasksFromFile(filePath)
                }
            }
            
            fileObserver?.startWatching(file)
            Log.d(TASK_VM_TAG, "File observer set up for: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TASK_VM_TAG, "Error setting up file observer: ${e.message}", e)
        }
    }
    
    /**
     * Trigger manual sync from file
     */
    fun syncFromFile() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            loadTasksFromFile(settings.taskFilePath)
        }
    }
    
    /**
     * Trigger manual sync to file
     */
    fun syncToFile() {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            writeTasksToFile(settings.taskFilePath)
        }
    }
    
    /**
     * Update file path and reinitialize sync
     */
    fun updateFilePath(newPath: String) {
        viewModelScope.launch {
            settingsRepository.updateTaskFilePath(newPath)
            initializeFileSync()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        fileObserver?.stopWatchingFile()
    }
} 