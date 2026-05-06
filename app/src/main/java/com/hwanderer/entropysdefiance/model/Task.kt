package com.hwanderer.entropysdefiance.model

import java.time.LocalDate

data class Task(
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon,
    val isDeadlineTask: Boolean = false,
    val deadline: LocalDate? = null,
    val doDate: LocalDate? = null
)

enum class TimeHorizon(val title: String) {
    Today("Today"),
    ThisWeek("This Week"),
    ThisMonth("This Month"),
    ThisYear("This Year"),
    ThisLife("This Life")
} 