package com.hwanderer.entropysdefiance.model

import java.time.LocalDateTime

/**
 * Model class representing a calendar event
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String? = null,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDay: Boolean = false,
    val color: Int? = null,
    val eventId: Long? = null,
    val calendarId: Long? = null
) 