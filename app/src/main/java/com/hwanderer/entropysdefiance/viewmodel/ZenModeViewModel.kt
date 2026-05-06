package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.EntropysDefianceApplication
import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
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
    
    // Get TaskViewModel to access tasks
    private val app = application as EntropysDefianceApplication
    
    private val _zenModeState = MutableStateFlow(ZenModeState())
    val zenModeState: StateFlow<ZenModeState> = _zenModeState.asStateFlow()
    
    private val _showDurationDialog = MutableStateFlow(false)
    val showDurationDialog: StateFlow<Boolean> = _showDurationDialog.asStateFlow()
    
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()
    
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    // We need a reference to TaskViewModel to complete tasks
    private var taskViewModel: TaskViewModel? = null
    
    fun setTaskViewModel(viewModel: TaskViewModel) {
        taskViewModel = viewModel
        // Observe tasks from TaskViewModel
        viewModelScope.launch {
            viewModel.tasks.collect { tasks ->
                _tasks.value = tasks
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
    
    fun startZenMode(duration: Int = 0) { // Duration parameter kept for compatibility but not used
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
        
        if (currentTask != null && taskViewModel != null) {
            // Mark the task as completed using TaskViewModel
            taskViewModel?.completeTask(currentTask.id)
            
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
