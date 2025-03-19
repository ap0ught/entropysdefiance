package com.example.entropysdefiance.model

data class Task(
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon
)

enum class TimeHorizon(val title: String) {
    Today("Today"),
    ThisWeek("This Week"),
    ThisMonth("This Month"),
    ThisYear("This Year"),
    ThisLife("This Life")
} 