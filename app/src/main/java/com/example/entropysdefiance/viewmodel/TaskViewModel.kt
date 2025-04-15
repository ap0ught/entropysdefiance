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
    
    val tasks = taskDao.getAllTasks().map { entities ->
        entities.map { it.toTask() }
    }

    fun addTask(title: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                timeHorizon = timeHorizon
            )
            taskDao.insertTask(task)
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
                    id = taskEntity.id,
                    title = taskEntity.title,
                    timeHorizon = newTimeHorizon
                )
                taskDao.insertTask(updatedTask)
            }
        }
    }
    
    fun updateTask(taskId: String, newTitle: String, timeHorizon: TimeHorizon) {
        viewModelScope.launch {
            val updatedTask = TaskEntity(
                id = taskId,
                title = newTitle,
                timeHorizon = timeHorizon
            )
            taskDao.insertTask(updatedTask)
        }
    }
} 