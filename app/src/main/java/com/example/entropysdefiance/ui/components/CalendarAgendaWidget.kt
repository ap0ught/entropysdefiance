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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

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

    // Create a constrained hitbox for the calendar widget that leaves space for the FAB and gesture areas
    // This creates a visual-only "cut" on the right side without affecting the actual layout
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Left part (main content area)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.8f) // Use 80% of width, leaving 20% for FAB area
        ) {
            // Improved scrollable surface with more elegant spacing
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 24.dp,
                    end = 16.dp, // Reduced right padding as we're already constrained
                    top = 16.dp,
                    bottom = 100.dp // Increase bottom padding to ensure content is visible above app drawer area
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Increased spacing between items for a more airy feel
            ) {
                // Optional: Add a subtle header for the upcoming events section
                item {
                    Text(
                        text = "UPCOMING EVENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    )
                }
                
                if (filteredEvents.isEmpty()) {
                    item {
                        // No upcoming events - more elegant empty state
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No upcoming events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
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
        
        // Right part (invisible, allows gestures to pass through to underlying gesture detectors)
        // This area intentionally has no content and no interaction handling
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.2f) // 20% of width for FAB area
                .align(Alignment.CenterEnd)
        )
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
        
        // For regular future events, show standard time with 24-hour format and include end time
        else -> {
            val startFormat = DateTimeFormatter.ofPattern("HH:mm")
            val endFormat = DateTimeFormatter.ofPattern("HH:mm")
            "${event.startTime.format(startFormat)} - ${event.endTime.format(endFormat)}"
        }
    }
    
    // Calculate event color or use a default
    val eventColor = event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    
    // For ongoing events, use a more prominent color, but keep event color for normal cases
    val statusColor = when {
        isEventOngoing -> MaterialTheme.colorScheme.tertiary
        minutesUntilStart in 1..360 -> MaterialTheme.colorScheme.secondary
        else -> eventColor
    }
    
    // New minimal design - replaces the Surface with border approach
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical colored indicator bar - thin elegant line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(50.dp)
                .background(
                    color = statusColor.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                )
        )
        
        // Event content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            // Event title - slightly more prominent
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Spacer for visual separation
            Spacer(modifier = Modifier.height(4.dp))
            
            // Time row with improved spacing
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date pill - smaller and more subtle
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
                
                // Subtle dot separator
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                
                // Time text - unified style
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            
            // Event location if available - only show if location exists
            event.location?.let { location ->
                if (location.isNotBlank()) {
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
} 