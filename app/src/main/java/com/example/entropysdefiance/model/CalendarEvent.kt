package com.example.entropysdefiance.model

import java.time.LocalDateTime

/**
 * Data class representing a calendar event
 */
data class CalendarEvent(
    val id: String,
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val location: String? = null,
    val description: String? = null,
    val isAllDay: Boolean = false,
    val color: Int? = null,
    val calendarId: String
) 