package com.example.entropysdefiance.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.data.TaskDatabase
import com.example.entropysdefiance.data.TaskEntity
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import com.example.entropysdefiance.utils.calculateTimeHorizonFromDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TaskDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    
    // SharedPreferences to track last update date
    private val prefs: SharedPreferences = application.getSharedPreferences(
        "task_horizon_updates", Context.MODE_PRIVATE
    )
    private val LAST_UPDATE_DATE_KEY = "last_deadline_update_date"
    
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
        }
    }

    fun completeTask(taskId: String) {
        viewModelScope.launch {
            taskDao.markTaskAsCompleted(taskId)
        }
    }
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskDao.deleteTask(taskId)
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
            entities.forEach { taskEntity ->
                if (taskEntity.isDeadlineTask && taskEntity.doDate != null) {
                    val task = taskEntity.toTask()
                    val doDate = taskEntity.doDate!! // Safe because we checked null above
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
                    }
                }
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
} 