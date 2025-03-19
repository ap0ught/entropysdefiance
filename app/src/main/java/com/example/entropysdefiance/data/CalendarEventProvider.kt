package com.example.entropysdefiance.data

import com.example.entropysdefiance.model.CalendarEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Provides sample calendar events data
 * In a real app, this would fetch events from the Calendar Provider ContentResolver
 */
object CalendarEventProvider {

    /**
     * Get a list of upcoming calendar events
     * @return List of calendar events ordered by start time
     */
    fun getUpcomingEvents(): List<CalendarEvent> {
        // Return sample events
        return generateSampleEvents()
    }

    /**
     * Generate sample calendar events for demo purposes
     */
    private fun generateSampleEvents(): List<CalendarEvent> {
        val now = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS)
        
        return listOf(
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Team Stand-up Meeting",
                startTime = now.plusHours(2),
                endTime = now.plusHours(3),
                location = "Virtual - Zoom",
                description = "Daily team sync-up",
                isAllDay = false,
                color = 0xFF1E88E5.toInt(),
                calendarId = "work@example.com"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Dentist Appointment",
                startTime = now.plusDays(1).withHour(14).withMinute(30),
                endTime = now.plusDays(1).withHour(15).withMinute(30),
                location = "Smile Dental Clinic",
                description = "Regular checkup",
                isAllDay = false,
                color = 0xFFE53935.toInt(),
                calendarId = "personal@example.com"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Gym Session",
                startTime = now.plusDays(2).withHour(7),
                endTime = now.plusDays(2).withHour(8),
                location = "Fitness Center",
                description = "Cardio and weights",
                isAllDay = false,
                color = 0xFF43A047.toInt(),
                calendarId = "personal@example.com"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Project Deadline",
                startTime = now.plusDays(3).withHour(18),
                endTime = now.plusDays(3).withHour(18),
                description = "Submit final deliverables",
                isAllDay = true,
                color = 0xFFEF6C00.toInt(),
                calendarId = "work@example.com"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Dinner with Friends",
                startTime = now.plusDays(5).withHour(19),
                endTime = now.plusDays(5).withHour(21).withMinute(30),
                location = "La Trattoria Restaurant",
                description = "Catch-up dinner with college friends",
                isAllDay = false,
                color = 0xFF8E24AA.toInt(),
                calendarId = "personal@example.com"
            ),
            CalendarEvent(
                id = UUID.randomUUID().toString(),
                title = "Flight to Boston",
                startTime = now.plusDays(10).withHour(10).withMinute(15),
                endTime = now.plusDays(10).withHour(12).withMinute(30),
                location = "Airport - Terminal B",
                description = "Flight AC123",
                isAllDay = false,
                color = 0xFF546E7A.toInt(),
                calendarId = "travel@example.com"
            )
        ).sortedBy { it.startTime }
    }
} 