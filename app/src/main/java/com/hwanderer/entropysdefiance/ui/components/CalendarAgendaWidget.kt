// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import android.text.format.DateFormat
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.CalendarEvent
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
    leftHanded: Boolean = false,
    maxEvents: Int = Int.MAX_VALUE // Allow all events with scrolling
) {
    val currentDate = remember { LocalDate.now() }
    val currentDateTime = remember { LocalDateTime.now() }
    
    // Group events by day. An event that already started (a multi-day or all-day event still
    // running) belongs under *today*, not its past start date — otherwise it lands in its own
    // earlier day-group and a divider wrongly separates it from today's other events. Clamping
    // the grouping key to today merges those ongoing events into the current day.
    val eventsByDay = remember(events, currentDate) {
        events
            .groupBy { maxOf(it.startTime.toLocalDate(), currentDate) }
            .mapValues { (_, dayEvents) ->
                dayEvents.sortedWith(compareBy<CalendarEvent> { it.isAllDay }.thenBy { it.startTime })
            }
            .toSortedMap() // Sort by date
    }
    
    // For a flat list when needed
    val filteredEvents = remember(eventsByDay) {
        eventsByDay.flatMap { it.value }
    }

    // Create a constrained hitbox for the calendar widget that leaves space for the FAB and gesture
    // areas. This is a visual-only "cut" on the side where the FAB/drawer-swipe live — the right
    // edge normally, mirrored to the left in left-handed mode.
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Main content area
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp), // Add bottom padding to the column itself
            contentPadding = PaddingValues(
                start = if (leftHanded) 80.dp else 24.dp,
                end = if (leftHanded) 24.dp else 80.dp, // Reserve the FAB/gesture side
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
                    textAlign = if (leftHanded) TextAlign.End else TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 0.dp)
                        .padding(bottom = 8.dp)
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
                            HorizontalDivider(
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
                                },
                                leftHanded = leftHanded
                            )
                        }
                    }
                }
            }
        }
        
        // Overlay that visually cuts off the list on the FAB/gesture side (right by default,
        // left in left-handed mode), without capturing clicks.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp) // Exact 80.dp to match the gesture detection area in MainActivity
                .align(if (leftHanded) Alignment.CenterStart else Alignment.CenterEnd)
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {} // Empty pointer input to prevent this box from intercepting clicks
        )
    }
}


@Composable
private fun CalendarEventItemContent(
    event: CalendarEvent,
    currentDate: LocalDate,
    currentDateTime: LocalDateTime,
    statusColor: Color,
    modifier: Modifier = Modifier,
    leftHanded: Boolean = false
) {
    val context = LocalContext.current
    val is24Hour = remember { DateFormat.is24HourFormat(context) }
    val clockFmt = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
    }
    val shortDayFmt = remember { DateTimeFormatter.ofPattern("EEE d") }
    val longDayFmt = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    val startDate = event.startTime.toLocalDate()
    // All-day events store an exclusive end (next-day midnight); the real last day is one less.
    val lastDate = if (event.isAllDay) {
        event.endTime.toLocalDate().minusDays(1)
    } else {
        event.endTime.toLocalDate()
    }
    val isMultiDay = lastDate.isAfter(startDate)
    val isOngoing = currentDateTime.isAfter(event.startTime) && currentDateTime.isBefore(event.endTime)

    // Friendly label for a single date: Today / Tomorrow / Yesterday, else "Mon, Jun 15".
    fun dayLabel(date: LocalDate): String = when (ChronoUnit.DAYS.between(currentDate, date)) {
        0L -> "Today"
        1L -> "Tomorrow"
        -1L -> "Yesterday"
        else -> date.format(longDayFmt)
    }

    fun durationShort(totalMinutes: Long): String {
        val mins = totalMinutes.coerceAtLeast(0)
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}min"
            h > 0 -> "${h}h"
            else -> "${m}min"
        }
    }

    // Optional trailing hint: how long an ongoing event still runs, or how soon one starts.
    val urgency: String? = when {
        event.isAllDay && isOngoing -> when (val d = ChronoUnit.DAYS.between(currentDate, lastDate)) {
            0L -> "ends today"
            1L -> "ends tomorrow"
            else -> "ends in $d days"
        }
        isOngoing -> {
            val minutesToEnd = ChronoUnit.MINUTES.between(currentDateTime, event.endTime)
            if (minutesToEnd < 24 * 60) {
                "ends in ${durationShort(minutesToEnd)}"
            } else when (val d = ChronoUnit.DAYS.between(currentDate, event.endTime.toLocalDate())) {
                1L -> "ends tomorrow"
                else -> "ends in $d days"
            }
        }
        !event.isAllDay -> {
            val minutesToStart = ChronoUnit.MINUTES.between(currentDateTime, event.startTime)
            when {
                minutesToStart == 0L -> "now"
                minutesToStart in 1..360 -> "in ${durationShort(minutesToStart)}"
                else -> null
            }
        }
        else -> null
    }

    // The "when" segments. Multi-day events carry the day on both ends so a span is never
    // read as a single backwards day. Pair.second marks an accent (anchor) segment.
    val segments: List<Pair<String, Boolean>> = buildList {
        if (isMultiDay) {
            if (event.isAllDay) {
                add("${startDate.format(shortDayFmt)} – ${lastDate.format(shortDayFmt)}" to true)
                add("All day" to false)
            } else {
                add(
                    ("${startDate.format(shortDayFmt)}, ${event.startTime.format(clockFmt)}" +
                        " → ${lastDate.format(shortDayFmt)}, ${event.endTime.format(clockFmt)}") to true
                )
            }
        } else {
            add(dayLabel(startDate) to true)
            add(
                (if (event.isAllDay) "All day"
                else "${event.startTime.format(clockFmt)}–${event.endTime.format(clockFmt)}") to false
            )
        }
        urgency?.let { add(it to false) }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val mutedColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    val separatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)

    val metaText = buildAnnotatedString {
        segments.forEachIndexed { index, (text, isAnchor) ->
            if (index > 0) {
                withStyle(SpanStyle(color = separatorColor)) { append("  ·  ") }
            }
            withStyle(
                SpanStyle(
                    color = if (isAnchor) statusColor else mutedColor,
                    fontWeight = if (isAnchor) FontWeight.Medium else FontWeight.Normal
                )
            ) { append(text) }
        }
    }

    val contentAlign = if (leftHanded) TextAlign.End else TextAlign.Start
    // The colored indicator bar sits on the outer edge — left normally, right in left-handed mode
    // where the agenda hugs the right side — and the text aligns to match.
    val indicatorBar = @Composable {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(50.dp)
                .background(
                    color = statusColor.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                )
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp) // Ensure minimum height for better tap target
                .padding(vertical = 12.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!leftHanded) indicatorBar()

            // Event content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (leftHanded) 0.dp else 12.dp,
                        end = if (leftHanded) 12.dp else 0.dp
                    )
            ) {
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
                    overflow = TextOverflow.Ellipsis,
                    textAlign = contentAlign,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Single meta line: day · time · urgency, consistent across every event type.
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = contentAlign,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            textAlign = contentAlign,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp)
                        )
                    }
                }
            }

            if (leftHanded) indicatorBar()
        }

        // Ongoing events get a thin full-width progress line at the bottom edge of the row,
        // showing how far through the event we currently are.
        if (isOngoing) {
            val totalSeconds = ChronoUnit.SECONDS.between(event.startTime, event.endTime)
                .coerceAtLeast(1L)
            val elapsedSeconds = ChronoUnit.SECONDS.between(event.startTime, currentDateTime)
                .coerceIn(0L, totalSeconds)
            val progress = elapsedSeconds.toFloat() / totalSeconds.toFloat()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(2.dp)
                    .background(statusColor.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(statusColor.copy(alpha = 0.85f))
                )
            }
        }
    }
} 