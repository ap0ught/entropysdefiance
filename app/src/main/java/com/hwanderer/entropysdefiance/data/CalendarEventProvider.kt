package com.hwanderer.entropysdefiance.data

import com.hwanderer.entropysdefiance.model.CalendarEvent
import com.hwanderer.entropysdefiance.model.DeviceCalendar
import java.time.LocalDateTime
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Provides mock calendar events for testing or when permissions aren't granted.
 * This is a fallback for the real CalendarRepository that fetches events from the device calendar.
 */
object CalendarEventProvider {
    /**
     * Get a list of mock calendars
     */
    fun getMockCalendars(): List<DeviceCalendar> {
        return listOf(
            DeviceCalendar(
                id = 1L,
                name = "Personal",
                color = Color(0xFF2196F3).toArgb(), // Blue
                account = "personal@example.com",
                owner = "personal@example.com",
                visible = true,
                selected = true
            ),
            DeviceCalendar(
                id = 2L,
                name = "Work",
                color = Color(0xFFF44336).toArgb(), // Red
                account = "work@example.com",
                owner = "work@example.com",
                visible = true,
                selected = true
            ),
            DeviceCalendar(
                id = 3L,
                name = "Family",
                color = Color(0xFF4CAF50).toArgb(), // Green
                account = "family@example.com",
                owner = "family@example.com",
                visible = true,
                selected = true
            ),
            DeviceCalendar(
                id = 4L,
                name = "Birthdays",
                color = Color(0xFFFF9800).toArgb(), // Orange
                account = "personal@example.com",
                owner = "personal@example.com",
                visible = true,
                selected = false
            )
        )
    }
    
    /**
     * Get a list of mock upcoming events
     */
    fun getUpcomingEvents(): List<CalendarEvent> {
        val now = LocalDateTime.now()
        
        return listOf(
            // Event 1: Today, in an hour (Personal calendar)
            CalendarEvent(
                id = 1L,
                title = "Team Meeting",
                location = "Conference Room 3",
                startTime = now.plusHours(1),
                endTime = now.plusHours(2),
                isAllDay = false,
                color = Color(0xFF2196F3).toArgb(), // Blue
                calendarId = 1L
            ),
            
            // Event 2: Tomorrow morning (Work calendar)
            CalendarEvent(
                id = 2L,
                title = "Dentist Appointment",
                location = "Bright Smile Dental",
                startTime = now.plusDays(1).withHour(9).withMinute(0),
                endTime = now.plusDays(1).withHour(10).withMinute(0),
                isAllDay = false,
                color = Color(0xFFF44336).toArgb(), // Red
                calendarId = 2L
            ),
            
            // Event 3: This weekend (Family calendar)
            CalendarEvent(
                id = 3L,
                title = "Weekend Hike",
                location = "Mountain Trail Park",
                startTime = now.plusDays(3).withHour(8).withMinute(0),
                endTime = now.plusDays(3).withHour(12).withMinute(0),
                isAllDay = false,
                color = Color(0xFF4CAF50).toArgb(), // Green
                calendarId = 3L
            ),
            
            // Event 4: Next week, all day (Work calendar)
            CalendarEvent(
                id = 4L,
                title = "Company Holiday",
                location = null,
                startTime = now.plusDays(7).withHour(0).withMinute(0),
                endTime = now.plusDays(7).withHour(23).withMinute(59),
                isAllDay = true,
                color = Color(0xFFF44336).toArgb(), // Red
                calendarId = 2L
            ),
            
            // Event 5: Later today, almost starting (Personal calendar)
            CalendarEvent(
                id = 5L,
                title = "Coffee with Alex",
                location = "Starbucks Downtown",
                startTime = now.plusMinutes(15),
                endTime = now.plusMinutes(45),
                isAllDay = false,
                color = Color(0xFF2196F3).toArgb(), // Blue
                calendarId = 1L
            ),
            
            // Event 6: Ongoing event (Work calendar)
            CalendarEvent(
                id = 6L,
                title = "Project Sprint",
                location = "Home Office",
                startTime = now.minusHours(1),
                endTime = now.plusHours(3),
                isAllDay = false,
                color = Color(0xFFF44336).toArgb(), // Red
                calendarId = 2L
            ),
            
            // Event 7: Birthday (Birthday calendar)
            CalendarEvent(
                id = 7L,
                title = "Sarah's Birthday",
                location = null,
                startTime = now.plusDays(5).withHour(0).withMinute(0),
                endTime = now.plusDays(5).withHour(23).withMinute(59),
                isAllDay = true,
                color = Color(0xFFFF9800).toArgb(), // Orange
                calendarId = 4L
            )
        )
    }
    
    /**
     * Get filtered events based on selected calendar IDs
     */
    fun getFilteredEvents(selectedCalendarIds: List<Long>): List<CalendarEvent> {
        val allEvents = getUpcomingEvents()
        
        // If no selection or empty selection, return all events
        if (selectedCalendarIds.isEmpty()) {
            return allEvents
        }
        
        // Filter events by selected calendar IDs
        return allEvents.filter { event ->
            event.calendarId in selectedCalendarIds
        }
    }
} 