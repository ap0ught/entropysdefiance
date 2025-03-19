package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.BorderStroke
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
    maxEvents: Int = 5
) {
    val currentDate = remember { LocalDate.now() }
    val currentDateTime = remember { LocalDateTime.now() }
    val filteredEvents = remember(events, currentDateTime) {
        events
            .sortedBy { it.startTime }
            .take(maxEvents)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (filteredEvents.isEmpty()) {
                // No upcoming events
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No upcoming events",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Event List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
    val accentColor = MaterialTheme.colorScheme.primary
    
    // For ongoing events, use a more prominent color
    val statusColor = when {
        isEventOngoing -> MaterialTheme.colorScheme.tertiary
        minutesUntilStart in 1..360 -> MaterialTheme.colorScheme.secondary
        else -> eventColor
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEventOngoing) statusColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Date and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Date indicator
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.15f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = statusColor
                    )
                }
                
                // Right side: Time info
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏰",
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Event title
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Event location if available
            event.location?.let { location ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍",
                        style = MaterialTheme.typography.labelMedium,
                        color = accentColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Event duration indicator with progress for ongoing events
            val durationMinutes = ChronoUnit.MINUTES.between(event.startTime, event.endTime)
            if (!event.isAllDay && durationMinutes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Calculate progress for ongoing events
                val progressFraction = if (isEventOngoing && durationMinutes > 0) {
                    val elapsedMinutes = ChronoUnit.MINUTES.between(event.startTime, currentDateTime)
                    (elapsedMinutes.toFloat() / durationMinutes).coerceIn(0f, 1f)
                } else {
                    0f // Default for future events
                }
                
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.2f)
                )
            }
        }
    }
} 