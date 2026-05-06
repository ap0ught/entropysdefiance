package com.hwanderer.entropysdefiance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hwanderer.entropysdefiance.EntropysDefianceApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Drives the welcome narration. The screen is shown once on first launch and
 * is replayable from Settings → Introduction. There is no per-step state — the
 * pager owns that locally.
 */
class TutorialViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = (application as EntropysDefianceApplication).settingsRepository

    private val _isWelcomeVisible = MutableStateFlow(false)
    val isWelcomeVisible: StateFlow<Boolean> = _isWelcomeVisible.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            if (!settings.tutorialCompleted) {
                _isWelcomeVisible.value = true
            }
        }
    }

    fun startTutorial() {
        _isWelcomeVisible.value = true
    }

    fun dismissTutorial() {
        _isWelcomeVisible.value = false
        viewModelScope.launch {
            settingsRepository.updateTutorialCompleted(true)
        }
    }
}
