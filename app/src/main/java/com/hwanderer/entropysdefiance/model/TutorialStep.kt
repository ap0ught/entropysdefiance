package com.hwanderer.entropysdefiance.model

enum class TooltipPosition {
    TOP, BOTTOM, LEFT, RIGHT, AUTO
}

data class TutorialStep(
    val id: String,
    val title: String,
    val description: String,
    val targetId: String,
    val position: TooltipPosition = TooltipPosition.AUTO
)