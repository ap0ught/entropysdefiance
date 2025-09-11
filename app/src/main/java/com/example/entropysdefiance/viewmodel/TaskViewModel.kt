package com.example.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.data.TaskDatabase
import com.example.entropysdefiance.data.TaskEntity
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TaskDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    
    val tasks = taskDao.getPendingTasks().map { entities ->
        entities.map { it.toTask() }
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
                isCompleted = false
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
                val updatedTask = TaskEntity(
                    autoId = taskEntity.autoId,
                    id = taskEntity.id,
                    title = taskEntity.title,
                    timeHorizon = newTimeHorizon,
                    createdAt = taskEntity.createdAt ?: java.time.LocalDateTime.now(),
                    isCompleted = taskEntity.isCompleted
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
                    isCompleted = taskEntity.isCompleted
                )
                taskDao.insertTask(updatedTask)
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