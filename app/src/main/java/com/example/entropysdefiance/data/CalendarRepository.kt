package com.example.entropysdefiance.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Instances
import android.provider.CalendarContract.Calendars
import com.example.entropysdefiance.model.CalendarEvent
import com.example.entropysdefiance.model.DeviceCalendar
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(private val context: Context) {

    companion object {
        // Projection array for calendar events query
        private val EVENT_PROJECTION = arrayOf(
            Instances._ID,                     // 0
            Instances.TITLE,                   // 1
            Instances.EVENT_LOCATION,          // 2
            Instances.BEGIN,                   // 3
            Instances.END,                     // 4
            Instances.ALL_DAY,                 // 5
            Instances.DISPLAY_COLOR,           // 6
            Instances.EVENT_ID,                // 7
            Instances.CALENDAR_ID              // 8 - Added calendar ID
        )
        
        // Indices for the event projection array
        private const val PROJECTION_ID_INDEX = 0
        private const val PROJECTION_TITLE_INDEX = 1
        private const val PROJECTION_LOCATION_INDEX = 2
        private const val PROJECTION_BEGIN_INDEX = 3
        private const val PROJECTION_END_INDEX = 4
        private const val PROJECTION_ALL_DAY_INDEX = 5
        private const val PROJECTION_COLOR_INDEX = 6
        private const val PROJECTION_EVENT_ID_INDEX = 7
        private const val PROJECTION_CALENDAR_ID_INDEX = 8
        
        // Projection array for calendars query
        private val CALENDAR_PROJECTION = arrayOf(
            Calendars._ID,                     // 0
            Calendars.CALENDAR_DISPLAY_NAME,   // 1
            Calendars.CALENDAR_COLOR,          // 2
            Calendars.ACCOUNT_NAME,            // 3
            Calendars.OWNER_ACCOUNT,           // 4
            Calendars.VISIBLE                  // 5
        )
        
        // Indices for the calendar projection array
        private const val CALENDAR_ID_INDEX = 0
        private const val CALENDAR_NAME_INDEX = 1
        private const val CALENDAR_COLOR_INDEX = 2
        private const val CALENDAR_ACCOUNT_INDEX = 3
        private const val CALENDAR_OWNER_INDEX = 4
        private const val CALENDAR_VISIBLE_INDEX = 5
    }
    
    /**
     * Fetch available calendars from the device
     */
    suspend fun getAvailableCalendars(): List<DeviceCalendar> = withContext(Dispatchers.IO) {
        val calendars = mutableListOf<DeviceCalendar>()
        
        context.contentResolver.query(
            Calendars.CONTENT_URI,
            CALENDAR_PROJECTION,
            null,
            null,
            Calendars.CALENDAR_DISPLAY_NAME + " ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(CALENDAR_ID_INDEX)
                val name = cursor.getString(CALENDAR_NAME_INDEX) ?: "Unnamed Calendar"
                val color = cursor.getInt(CALENDAR_COLOR_INDEX)
                val account = cursor.getString(CALENDAR_ACCOUNT_INDEX) ?: ""
                val owner = cursor.getString(CALENDAR_OWNER_INDEX) ?: ""
                val visible = cursor.getInt(CALENDAR_VISIBLE_INDEX) == 1
                
                calendars.add(
                    DeviceCalendar(
                        id = id,
                        name = name,
                        color = color,
                        account = account,
                        owner = owner,
                        visible = visible,
                        selected = visible // Default selection based on calendar visibility
                    )
                )
            }
        }
        
        return@withContext calendars
    }
    
    /**
     * Fetch calendar events within the specified time range, filtered by selected calendars
     * @param daysInFuture Number of days to look ahead (1 for today, 7 for a week, etc.)
     * @param selectedCalendarIds List of calendar IDs to include (empty list means all calendars)
     * @return List of CalendarEvent objects
     */
    suspend fun getEvents(
        daysInFuture: Int = 7,
        selectedCalendarIds: List<Long> = emptyList()
    ): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val end = now + (daysInFuture * 24 * 60 * 60 * 1000) // milliseconds in days
        
        val builder: Uri.Builder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now)
        ContentUris.appendId(builder, end)
        
        val events = mutableListOf<CalendarEvent>()
        
        // Build selection string if we're filtering by calendar IDs
        val selection = if (selectedCalendarIds.isNotEmpty()) {
            val calendarSelection = selectedCalendarIds.joinToString(",") { it.toString() }
            "${Instances.CALENDAR_ID} IN ($calendarSelection)"
        } else null
        
        context.contentResolver.query(
            builder.build(),
            EVENT_PROJECTION,
            selection,
            null,
            Instances.BEGIN + " ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val event = cursorToCalendarEvent(cursor)
                events.add(event)
            }
        }
        
        return@withContext events
    }
    
    /**
     * Convert a cursor row to a CalendarEvent object
     */
    private fun cursorToCalendarEvent(cursor: Cursor): CalendarEvent {
        val id = cursor.getLong(PROJECTION_ID_INDEX)
        val title = cursor.getString(PROJECTION_TITLE_INDEX) ?: "Untitled Event"
        val location = cursor.getString(PROJECTION_LOCATION_INDEX)
        val beginMillis = cursor.getLong(PROJECTION_BEGIN_INDEX)
        val endMillis = cursor.getLong(PROJECTION_END_INDEX)
        val allDay = cursor.getInt(PROJECTION_ALL_DAY_INDEX) == 1
        val color = cursor.getInt(PROJECTION_COLOR_INDEX)
        val eventId = cursor.getLong(PROJECTION_EVENT_ID_INDEX)
        val calendarId = cursor.getLong(PROJECTION_CALENDAR_ID_INDEX)
        
        // Convert milliseconds to LocalDateTime
        val startTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(beginMillis),
            ZoneId.systemDefault()
        )
        
        val endTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(endMillis),
            ZoneId.systemDefault()
        )
        
        return CalendarEvent(
            id = id,
            title = title,
            location = location,
            startTime = startTime,
            endTime = endTime,
            isAllDay = allDay,
            color = color,
            eventId = eventId,
            calendarId = calendarId // Store calendar ID with each event
        )
    }
} 