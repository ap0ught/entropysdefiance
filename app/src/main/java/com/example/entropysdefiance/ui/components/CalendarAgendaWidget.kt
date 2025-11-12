package com.example.entropysdefiance.ui.components

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.ColorMatrix

/**
 * Utility function to determine if a color is dark based on luminance
 */
private fun Color.isDark() = this.luminance() < 0.5f

/**
 * Calculate contrast ratio between two colors
 * Based on WCAG 2.0 contrast guidelines
 */
private fun calculateContrastRatio(color1: Color, color2: Color): Float {
    val l1 = color1.luminance() + 0.05f
    val l2 = color2.luminance() + 0.05f
    
    return if (l1 > l2) l1/l2 else l2/l1
}

/**
 * Utility function to enhance text contrast based on background and text colors
 * Returns the appropriate text color for better visibility
 */
private fun getContrastEnhancedTextStyle(
    baseStyle: TextStyle,
    textColor: Color,
    backgroundColor: Color
): TextStyle {
    // Calculate contrast ratio
    val contrastRatio = calculateContrastRatio(textColor, backgroundColor)
    
    // WCAG 2.0 recommends at least 4.5:1 for normal text
    val minimumContrastRatio = 4.5f
    
    // If contrast is sufficient, use the original color
    if (contrastRatio >= minimumContrastRatio) {
        return baseStyle.copy(color = textColor)
    }
    
    // Otherwise, choose either white or black based on background luminance
    val isDarkBackground = backgroundColor.isDark()
    
    return baseStyle.copy(
        color = if (isDarkBackground) Color.White else Color.Black
    )
}

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
    
    // Group events by day for easier processing when rendering
    val eventsByDay = remember(events, currentDateTime) {
        events
            .groupBy { it.startTime.toLocalDate() }
            .mapValues { (_, dayEvents) ->
                dayEvents.sortedWith(compareBy<CalendarEvent> { it.isAllDay }.thenBy { it.startTime })
            }
            .toSortedMap() // Sort by date
    }
    
    // For a flat list when needed
    val filteredEvents = remember(eventsByDay) {
        eventsByDay.flatMap { it.value }
    }

    // Create a constrained hitbox for the calendar widget that leaves space for the FAB and gesture areas
    // This creates a visual-only "cut" on the right side without affecting the actual layout
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp), // Add bottom padding to the column itself
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 80.dp, // Increased end padding to match the overlay width
                top = 24.dp, // Increase top padding for better visibility
                bottom = 120.dp // Substantial bottom padding to ensure content is visible above app drawer
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Optional: Add a subtle header for the upcoming events section
            item {
                Text(
                    text = "UPCOMING EVENTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(start = 8.dp, bottom = 8.dp)
                        .clickable { 
                            android.util.Log.d("CalendarAgendaWidget", "Header clicked")
                        }
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
                // Iterate through each day's events and add dividers between days
                var isFirstDay = true
                
                eventsByDay.forEach { (day, dayEvents) ->
                    // Add a divider before each day except the first one
                    if (!isFirstDay) {
                        item {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        isFirstDay = false
                    }
                    
                    // Add all events for this day
                    items(dayEvents) { event ->
                        // Use Surface to improve the clickable area definition
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    android.util.Log.d("CalendarAgendaWidget", "Surface clicked: ${event.title}")
                                    onEventClick(event) 
                                },
                            color = MaterialTheme.colorScheme.background,
                            tonalElevation = 0.dp
                        ) {
                            // Use a non-clickable version of the item to avoid double-click handlers
                            CalendarEventItemContent(
                                event = event,
                                currentDate = currentDate,
                                currentDateTime = currentDateTime,
                                statusColor = when {
                                    // For ongoing events, use the event's color but with higher opacity
                                    currentDateTime.isAfter(event.startTime) && 
                                    currentDateTime.isBefore(event.endTime) -> 
                                        event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.tertiary
                                    // For events starting soon (in the next 6 hours), use the event's color 
                                    event.startTime.isAfter(currentDateTime) && 
                                    ChronoUnit.MINUTES.between(currentDateTime, event.startTime) <= 360 -> 
                                        event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary
                                    // For all other events, use the event's original color
                                    else -> event.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Add an overlay to the right side that doesn't capture clicks but visually cuts off the list
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp) // Exact 80.dp to match the gesture detection area in MainActivity
                .align(Alignment.CenterEnd)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {} // Empty pointer input to prevent this box from intercepting clicks
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
    
    // Format time components - dynamic for close events (< 6 hours), static for others
    val (relativeTimeText, actualTimeText) = when {
        // For all-day events - check first to avoid time calculations
        event.isAllDay -> Pair("All day", null)
        
        // If event is ongoing, show "ends in X hours Y minutes" + actual times
        isEventOngoing -> {
            val relativeTime = when {
                hoursUntilEnd > 0 -> "ends in ${hoursUntilEnd}h ${remainingMinutesUntilEnd}min"
                else -> "ends in ${remainingMinutesUntilEnd}min"
            }
            val actualTimes = "${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            Pair(relativeTime, actualTimes)
        }
        
        // If event is within 6 hours, show "in X hours Y minutes" + actual times
        minutesUntilStart in 1..360 -> {
            val relativeTime = when {
                hoursUntilStart > 0 -> "in ${hoursUntilStart}h ${remainingMinutesUntilStart}min"
                else -> "in ${remainingMinutesUntilStart}min"
            }
            val actualTimes = "${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            Pair(relativeTime, actualTimes)
        }
        
        // For regular future events, show standard time with 24-hour format and include end time
        else -> {
            val startFormat = DateTimeFormatter.ofPattern("HH:mm")
            val endFormat = DateTimeFormatter.ofPattern("HH:mm")
            val timeText = "${event.startTime.format(startFormat)} - ${event.endTime.format(endFormat)}"
            Pair(timeText, null)
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
            .defaultMinSize(minHeight = 64.dp) // Ensure minimum height for better tap target
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true),
                onClick = { 
                    android.util.Log.d("CalendarEventItem", "Event clicked: ${event.title}")
                    onClick()
                }
            )
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
                    text = relativeTimeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                
                // Show actual times with visual separator if available
                actualTimeText?.let { actualTimes ->
                    // Subtle dot separator
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                    
                    // Actual time text
                    Text(
                        text = actualTimes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
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

@Composable
private fun CalendarEventItemContent(
    event: CalendarEvent,
    currentDate: LocalDate,
    currentDateTime: LocalDateTime,
    statusColor: Color,
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
    
    // Format time components - dynamic for close events (< 6 hours), static for others
    val (relativeTimeText, actualTimeText) = when {
        // For all-day events - check first to avoid time calculations
        event.isAllDay -> Pair("All day", null)
        
        // If event is ongoing, show "ends in X hours Y minutes" + actual times
        isEventOngoing -> {
            val relativeTime = when {
                hoursUntilEnd > 0 -> "ends in ${hoursUntilEnd}h ${remainingMinutesUntilEnd}min"
                else -> "ends in ${remainingMinutesUntilEnd}min"
            }
            val actualTimes = "${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            Pair(relativeTime, actualTimes)
        }
        
        // If event is within 6 hours, show "in X hours Y minutes" + actual times
        minutesUntilStart in 1..360 -> {
            val relativeTime = when {
                hoursUntilStart > 0 -> "in ${hoursUntilStart}h ${remainingMinutesUntilStart}min"
                else -> "in ${remainingMinutesUntilStart}min"
            }
            val actualTimes = "${event.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${event.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}"
            Pair(relativeTime, actualTimes)
        }
        
        // For regular future events, show standard time with 24-hour format and include end time
        else -> {
            val startFormat = DateTimeFormatter.ofPattern("HH:mm")
            val endFormat = DateTimeFormatter.ofPattern("HH:mm")
            val timeText = "${event.startTime.format(startFormat)} - ${event.endTime.format(endFormat)}"
            Pair(timeText, null)
        }
    }
    
    // New minimal design - replaces the Surface with border approach
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp) // Ensure minimum height for better tap target
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
            // Get the background color for contrast calculation
            val backgroundColor = MaterialTheme.colorScheme.background
            
            // Event title - with enhanced contrast if needed
            val titleStyle = getContrastEnhancedTextStyle(
                baseStyle = MaterialTheme.typography.bodyLarge,
                textColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = backgroundColor
            )
            
            Text(
                text = event.title,
                style = titleStyle,
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
                // Date pill - with contrast enhancement and pill-shaped background
                val dateTextStyle = getContrastEnhancedTextStyle(
                    baseStyle = MaterialTheme.typography.labelSmall,
                    textColor = statusColor,
                    backgroundColor = backgroundColor
                )
                
                // If the background is dark and the event color is also dark, make a more visible pill
                val pillColor = if (backgroundColor.isDark() && statusColor.isDark()) {
                    // Use a lightened version of the status color
                    Color.White.copy(alpha = 0.2f)
                } else {
                    // Otherwise use the status color with low opacity
                    statusColor.copy(alpha = 0.15f)
                }
                
                Surface(
                    color = pillColor,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = dateText,
                        style = dateTextStyle,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Subtle dot separator
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                
                // Time text - unified style with contrast enhancement
                val timeTextStyle = getContrastEnhancedTextStyle(
                    baseStyle = MaterialTheme.typography.labelSmall,
                    textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    backgroundColor = backgroundColor
                )
                
                Text(
                    text = relativeTimeText,
                    style = timeTextStyle
                )
                
                // Show actual times with visual separator if available
                actualTimeText?.let { actualTimes ->
                    // Subtle dot separator
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                    
                    // Actual time text with contrast enhancement
                    val actualTimeTextStyle = getContrastEnhancedTextStyle(
                        baseStyle = MaterialTheme.typography.labelSmall,
                        textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        backgroundColor = backgroundColor
                    )
                    
                    Text(
                        text = actualTimes,
                        style = actualTimeTextStyle
                    )
                }
            }
            
            // Event location if available - only show if location exists
            event.location?.let { location ->
                if (location.isNotBlank()) {
                    val locationTextStyle = getContrastEnhancedTextStyle(
                        baseStyle = MaterialTheme.typography.bodySmall,
                        textColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        backgroundColor = backgroundColor
                    )
                    
                    Text(
                        text = location,
                        style = locationTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
} 