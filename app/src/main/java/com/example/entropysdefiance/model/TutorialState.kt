package com.example.entropysdefiance.model

import androidx.compose.ui.geometry.Rect

data class TutorialState(
    val isActive: Boolean = false,
    val currentStepIndex: Int? = null,
    val targets: Map<String, Rect> = emptyMap()
) {
    fun getCurrentStep(steps: List<TutorialStep>): TutorialStep? {
        return currentStepIndex?.let { idx ->
            if (idx in steps.indices) steps[idx] else null
        }
    }

    fun getCurrentTargetBounds(steps: List<TutorialStep>): Rect? {
        return getCurrentStep(steps)?.let { targets[it.targetId] }
    }
}