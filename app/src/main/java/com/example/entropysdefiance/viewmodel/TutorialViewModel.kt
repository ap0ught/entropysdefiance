package com.example.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.entropysdefiance.EntropysDefianceApplication
import com.example.entropysdefiance.model.TutorialState
import com.example.entropysdefiance.model.TutorialStep
import com.example.entropysdefiance.model.TooltipPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TutorialViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = (application as EntropysDefianceApplication).settingsRepository

    private val _tutorialState = MutableStateFlow(TutorialState())
    val tutorialState: StateFlow<TutorialState> = _tutorialState.asStateFlow()

    private lateinit var tutorialSteps: List<TutorialStep>
    private val completedActions = mutableSetOf<String>()

    init {
        initializeTutorialSteps()
    }

    private fun initializeTutorialSteps() {
        tutorialSteps = listOf(
            TutorialStep(
                id = "tap_fab",
                title = "Welcome to Entropy's Defiance",
                description = "Tap the + button to expand the menu.",
                targetId = "main_fab",
                position = TooltipPosition.BOTTOM
            ),
            TutorialStep(
                id = "tap_add_task",
                title = "Add Your First Task",
                description = "Tap the 'Add Task' button to create your first task.",
                targetId = "add_task_button",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "create_task_1",
                title = "Create First Task",
                description = "Enter a title and select 'Today'. Tap Add.",
                targetId = "add_button",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "create_task_2",
                title = "Create Second Task",
                description = "Add another task, but select 'This Week' this time.",
                targetId = "add_button",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "swipe_task",
                title = "Move Tasks Between Horizons",
                description = "Swipe the Today task right to move it to This Week. The view auto-switches.",
                targetId = "task_list",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "enter_zen_mode",
                title = "Focus Mode",
                description = "Long press a task to enter Zen Mode for distraction-free work.",
                targetId = "task_item",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "tap_horizon_title",
                title = "Open Calendar",
                description = "Tap the date/time title to open your calendar app.",
                targetId = "horizon_title",
                position = TooltipPosition.BOTTOM
            ),
            TutorialStep(
                id = "longpress_horizon_title",
                title = "Task Alignment",
                description = "Long press the title to open the alignment screen for planning.",
                targetId = "horizon_title",
                position = TooltipPosition.BOTTOM
            ),
            TutorialStep(
                id = "open_app_drawer",
                title = "Access Your Apps",
                description = "Swipe up from the bottom edge to open the app drawer.",
                targetId = "bottom_edge",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "search_apps",
                title = "Quick App Search",
                description = "Start typing to search for apps. The search auto-opens.",
                targetId = "app_drawer_search",
                position = TooltipPosition.BOTTOM
            ),
            TutorialStep(
                id = "launch_app",
                title = "Launch Apps",
                description = "Tap an app in the drawer to launch it.",
                targetId = "app_drawer_list",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "double_tap_dialer",
                title = "Quick Dialer",
                description = "Double-tap anywhere on the main screen to open the dialer.",
                targetId = "main_content",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "open_settings",
                title = "Settings & Customization",
                description = "Long press the main screen to open settings.",
                targetId = "main_content",
                position = TooltipPosition.TOP
            ),
            TutorialStep(
                id = "completion",
                title = "You're Ready!",
                description = "You now know the basics. Enjoy using Entropy's Defiance!",
                targetId = "",
                position = TooltipPosition.AUTO
            )
        )
    }

    fun startTutorial() {
        completedActions.clear()
        _tutorialState.value = TutorialState(
            isActive = true,
            currentStepIndex = 0
        )
        viewModelScope.launch {
            settingsRepository.updateTutorialCompleted(false)
        }
    }

    fun exitTutorial() {
        _tutorialState.value = TutorialState(isActive = false)
        markTutorialCompleted()
    }

    private fun markTutorialCompleted() {
        viewModelScope.launch {
            settingsRepository.updateTutorialCompleted(true)
        }
    }

    fun getCurrentStep(): TutorialStep? {
        return _tutorialState.value.currentStepIndex?.let { tutorialSteps.getOrNull(it) }
    }

    fun isTutorialActive(): Boolean = _tutorialState.value.isActive

    fun getCurrentStepIndex(): Int = _tutorialState.value.currentStepIndex ?: 0

    fun getAllSteps(): List<TutorialStep> = tutorialSteps

    fun advanceStep() {
        val currentIndex = _tutorialState.value.currentStepIndex ?: return
        if (currentIndex < tutorialSteps.size - 1) {
            _tutorialState.value = _tutorialState.value.copy(
                currentStepIndex = currentIndex + 1
            )
        } else {
            completeTutorial()
        }
    }

    private fun completeTutorial() {
        _tutorialState.value = TutorialState(isActive = false)
        markTutorialCompleted()
    }

    fun onFabTapped() = advanceIfMatches("tap_fab")
    fun onAddTaskButtonTapped() = advanceIfMatches("tap_add_task")
    fun onTaskCreated(timeHorizon: String) {
        val currentStep = getCurrentStep() ?: return
        if (currentStep.id == "create_task_1" && timeHorizon == "Today") {
            advanceIfMatches("create_task_1")
        } else if (currentStep.id == "create_task_2" && timeHorizon == "ThisWeek") {
            advanceIfMatches("create_task_2")
        }
    }
    fun onTaskSwiped() = advanceIfMatches("swipe_task")
    fun onZenModeStarted() = advanceIfMatches("enter_zen_mode")
    fun onHorizonTitleTapped() = advanceIfMatches("tap_horizon_title")
    fun onHorizonTitleLongPressed() = advanceIfMatches("longpress_horizon_title")
    fun onAppDrawerOpened() = advanceIfMatches("open_app_drawer")
    fun onAppDrawerSearchFocused() = advanceIfMatches("search_apps")
    fun onAppLaunched() = advanceIfMatches("launch_app")
    fun onDoubleTappedMainScreen() = advanceIfMatches("double_tap_dialer")
    fun onSettingsOpened() = advanceIfMatches("open_settings")

    private fun advanceIfMatches(actionId: String): Boolean {
        if (!_tutorialState.value.isActive) return false
        val currentStep = getCurrentStep() ?: return false
        if (currentStep.id == actionId && actionId !in completedActions) {
            completedActions.add(actionId)
            advanceStep()
            return true
        }
        return false
    }
}
