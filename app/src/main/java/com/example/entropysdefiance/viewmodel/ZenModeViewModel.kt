package com.example.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.data.TaskDao
import com.example.entropysdefiance.data.TaskDatabase
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.LocalDateTime

data class ZenModeState(
    val isActive: Boolean = false,
    val currentTask: Task? = null,
    val startTime: LocalDateTime? = null
)

class ZenModeViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val taskDao: TaskDao = TaskDatabase.getDatabase(application).taskDao()
    
    private val _zenModeState = MutableStateFlow(ZenModeState())
    val zenModeState: StateFlow<ZenModeState> = _zenModeState.asStateFlow()
    
    private val _showDurationDialog = MutableStateFlow(false)
    val showDurationDialog: StateFlow<Boolean> = _showDurationDialog.asStateFlow()
    
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()
    
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    init {
        // Load pending tasks from database
        viewModelScope.launch {
            taskDao.getPendingTasks().collect { taskEntities ->
                _tasks.value = taskEntities.map { it.toTask() }
                updateCurrentTask()
            }
        }
    }
    
    fun showDurationDialog() {
        _showDurationDialog.value = true
    }
    
    fun showDurationDialogForTask(task: Task) {
        _selectedTask.value = task
        _showDurationDialog.value = true
    }
    
    fun hideDurationDialog() {
        _showDurationDialog.value = false
        _selectedTask.value = null
    }
    
    fun startZenModeDirectly(task: Task) {
        _zenModeState.value = ZenModeState(
            isActive = true,
            currentTask = task,
            startTime = LocalDateTime.now()
        )
    }
    
    fun startZenMode(_: Int = 0) { // Duration parameter kept for compatibility but not used
        val selectedTask = _selectedTask.value ?: findNextTask()
        if (selectedTask != null) {
            _zenModeState.value = ZenModeState(
                isActive = true,
                currentTask = selectedTask,
                startTime = LocalDateTime.now()
            )
        }
        hideDurationDialog()
    }
    
    private fun exitZenMode() {
        _zenModeState.value = ZenModeState()
    }
    
    fun completeCurrentTask() {
        val currentState = _zenModeState.value
        val currentTask = currentState.currentTask
        
        if (currentTask != null) {
            // Mark the task as completed instead of deleting it
            viewModelScope.launch {
                taskDao.markTaskAsCompleted(currentTask.id)
            }
            
            // Exit zen mode after completing the task
            exitZenMode()
        }
    }
    
    private fun findNextTask(): Task? {
        val allTasks = _tasks.value
        
        // Priority order: Today -> This Week -> This Month -> This Year -> This Life
        val priorityOrder = listOf(
            TimeHorizon.Today,
            TimeHorizon.ThisWeek,
            TimeHorizon.ThisMonth,
            TimeHorizon.ThisYear,
            TimeHorizon.ThisLife
        )
        
        for (horizon in priorityOrder) {
            val taskInHorizon = allTasks.firstOrNull { it.timeHorizon == horizon }
            if (taskInHorizon != null) {
                return taskInHorizon
            }
        }
        
        return null
    }
    
    private fun updateCurrentTask() {
        val currentState = _zenModeState.value
        if (currentState.isActive && currentState.currentTask == null) {
            val nextTask = findNextTask()
            _zenModeState.value = currentState.copy(
                currentTask = nextTask
            )
        }
    }
    
    
}
