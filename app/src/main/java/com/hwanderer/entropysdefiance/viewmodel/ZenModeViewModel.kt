// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.EntropysDefianceApplication
import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
import com.hwanderer.entropysdefiance.utils.ZenModeManager
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

    private val zenModeManager = ZenModeManager(application)
    
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
        applyDndIfEnabled()
    }
    
    fun startZenMode(duration: Int = 0) { // Duration parameter kept for compatibility but not used
        val selectedTask = _selectedTask.value ?: findNextTask()
        if (selectedTask != null) {
            _zenModeState.value = ZenModeState(
                isActive = true,
                currentTask = selectedTask,
                startTime = LocalDateTime.now()
            )
            applyDndIfEnabled()
        }
        hideDurationDialog()
    }
    
    private fun exitZenMode() {
        _zenModeState.value = ZenModeState()
        zenModeManager.restoreDnd()
    }

    /**
     * Switches on Do Not Disturb for the session, if the user opted in.
     *
     * Silently does nothing when the setting is off or "Do Not Disturb access" was never
     * granted — the Permissions screen is where that state is visible and fixable, and a
     * focus session is the wrong moment to interrupt someone with a permission prompt.
     */
    private fun applyDndIfEnabled() {
        viewModelScope.launch {
            val settings = app.settingsRepository.settingsFlow.first()
            if (settings.enableDndInZenMode) {
                zenModeManager.enableDnd(totalSilence = settings.dndTotalSilence)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Never leave the phone silenced because the app went away mid-session. The cold-start
        // check in EntropysDefianceApplication covers the case where we are killed outright
        // and this never runs.
        zenModeManager.restoreDnd()
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
