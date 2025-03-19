package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.CalendarEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * A calendar agenda widget that displays upcoming events.
 * Design inspired by Calendar Notify app.
 */
@Composable
fun CalendarAgendaWidget(
    events: List<CalendarEvent>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
    maxEvents: Int = Int.MAX_VALUE // Allow all events with scrolling
) {
    val currentDate = remember { LocalDate.now() }
    val currentDateTime = remember { LocalDateTime.now() }
    val filteredEvents = remember(events, currentDateTime) {
        // Group events by day, then sort all-day events to the end of each day's group
        events
            .groupBy { it.startTime.toLocalDate() }
            .flatMap { (_, dayEvents) ->
                dayEvents.sortedWith(compareBy<CalendarEvent> { it.isAllDay }.thenBy { it.startTime })
            }
            // No longer limiting events as scrolling is enabled
    }

    // Use scrollable surface for events - removing outer border to match task list styling
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight() // Take all remaining height
            .padding(bottom = 80.dp), // Add bottom padding to avoid conflict with app drawer gesture area
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp), // Match task list padding
        verticalArrangement = Arrangement.spacedBy(12.dp) // Match task list spacing
    ) {
        if (filteredEvents.isEmpty()) {
            item {
                // No upcoming events
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(filteredEvents) { event ->
                CalendarEventItem(
                    event = event,
                    currentDate = currentDate,
                    currentDateTime = currentDateTime,
                    onClick = { onEventClick(event) }
                )
            }
        }
    }
}

@Composable
private fun CalendarEventItem(
    event: CalendarEvent,
    currentDate: LocalDate,
    currentDateTime: LocalDateTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventDate = event.startTime.toLocalDate()
    val dayDifference = ChronoUnit.DAYS.between(currentDate, eventDate)
    
    // Format date based on how far in the future it is
    val dateText = when {
        dayDifference == 0L -> "Today"
        dayDifference == 1L -> "Tomorrow"
        dayDifference in 2..6 -> event.startTime.format(DateTimeFormatter.ofPattern("EEEE")) // Day name
        else -> event.startTime.format(DateTimeFormatter.ofPattern("EEE, MMM d")) // Short format
    }
    
    // Determine if the event is ongoing
    val isEventOngoing = currentDateTime.isAfter(event.startTime) && currentDateTime.isBefore(event.endTime)
    
    // Calculate minutes until event start or end
    val minutesUntilStart = ChronoUnit.MINUTES.between(currentDateTime, event.startTime)
    val minutesUntilEnd = ChronoUnit.MINUTES.between(currentDateTime, event.endTime)
    
    // Calculate hours and minutes for display
    val hoursUntilStart = minutesUntilStart / 60
    val remainingMinutesUntilStart = minutesUntilStart % 60
    
    val hoursUntilEnd = minutesUntilEnd / 60
    val remainingMinutesUntilEnd = minutesUntilEnd % 60
    
    // Format time - dynamic for close events (< 6 hours), static for others
    val timeText = when {
        // If event is ongoing, show "ends in X hours Y minutes"
        isEventOngoing -> {
            when {
                hoursUntilEnd > 0 -> "ends in ${hoursUntilEnd}h ${remainingMinutesUntilEnd}min"
                else -> "ends in ${remainingMinutesUntilEnd}min"
            }
        }
        
        // If event is within 6 hours, show "in X hours Y minutes"
        minutesUntilStart in 1..360 -> {
            when {
                hoursUntilStart > 0 -> "in ${hoursUntilStart}h ${remainingMinutesUntilStart}min"
                else -> "in ${remainingMinutesUntilStart}min"
            }
        }
        
        // For all-day events
        event.isAllDay -> "All day"
        
        // For regular future events, show standard time
        else -> event.startTime.format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    // Calculate event color or use a default
    val eventColor = event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    
    // For ongoing events, use a more prominent color, but keep event color for normal cases
    val statusColor = when {
        isEventOngoing -> MaterialTheme.colorScheme.tertiary
        minutesUntilStart in 1..360 -> MaterialTheme.colorScheme.secondary
        else -> eventColor
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clickable { onClick() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event title and details (left side)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Event title - using same style as task title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Date and time indicators
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date indicator with event color
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = eventColor.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = eventColor
                        )
                    }
                    
                    // Time info
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                // Event location if available - only show if location exists
                event.location?.let { location ->
                    if (location.isNotBlank()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
} 