package com.example.entropysdefiance.utils

import com.example.entropysdefiance.model.CalendarEvent
import com.example.entropysdefiance.model.FreeTimeMetric
import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

/**
 * Utility object for calculating free time metrics based on calendar events
 */
object CalendarTimeCalculator {
    
    /**
     * Calculate free time metric for a given time horizon
     * 
     * @param events List of calendar events to consider
     * @param timeHorizon The time horizon to calculate for
     * @param productivityRatio Ratio of productive time (0.0 to 1.0, default 0.5)
     * @param now Reference time (default: current time)
     * @return FreeTimeMetric with calculated values
     */
    fun calculateFreeTime(
        events: List<CalendarEvent>,
        timeHorizon: TimeHorizon,
        productivityRatio: Float = 0.5f,
        now: LocalDateTime = LocalDateTime.now()
    ): FreeTimeMetric {
        // 1. Get end of time horizon (start is always NOW)
        val horizonEnd = getTimeHorizonEnd(timeHorizon, now)
        
        // 2. Filter events: only future events within horizon
        val relevantEvents = events.filter { 
            it.endTime > now && it.startTime < horizonEnd
        }
        
        // 3. Clip events to the [now, horizonEnd] window
        val clippedEvents = relevantEvents.map { event ->
            CalendarEvent(
                id = event.id,
                title = event.title,
                location = event.location,
                startTime = maxOf(event.startTime, now),
                endTime = minOf(event.endTime, horizonEnd),
                isAllDay = event.isAllDay,
                color = event.color,
                eventId = event.eventId,
                calendarId = event.calendarId
            )
        }
        
        // 4. Calculate non-overlapping event hours (24h basis)
        val eventHours = calculateNonOverlappingDuration(clippedEvents)
        
        // 5. Calculate total hours from now to horizon end
        val totalHours = ChronoUnit.MINUTES.between(now, horizonEnd).toDouble() / 60.0
        
        // 6. Calculate free hours on 24h basis
        val freeHours24h = max(0.0, totalHours - eventHours)
        
        // 7. Apply productivity ratio to get productive hours available
        val productiveHoursAvailable = freeHours24h * productivityRatio
        
        return FreeTimeMetric(
            totalHoursRemaining = totalHours,
            scheduledHours = eventHours,
            freeHours24h = freeHours24h,
            productiveHoursAvailable = productiveHoursAvailable
        )
    }
    
    /**
     * Calculate total duration of events while handling overlaps
     * If events overlap, the overlapping time is only counted once
     * 
     * @param events List of calendar events (should already be filtered and clipped)
     * @return Total non-overlapping hours
     */
    private fun calculateNonOverlappingDuration(events: List<CalendarEvent>): Double {
        if (events.isEmpty()) return 0.0
        
        // Sort events by start time
        val sortedEvents = events.sortedBy { it.startTime }
        
        // Merge overlapping intervals
        val mergedIntervals = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
        var currentInterval = sortedEvents[0].startTime to sortedEvents[0].endTime
        
        for (i in 1 until sortedEvents.size) {
            val event = sortedEvents[i]
            
            if (event.startTime <= currentInterval.second) {
                // Events overlap - merge them by extending the end time
                currentInterval = currentInterval.first to maxOf(currentInterval.second, event.endTime)
            } else {
                // No overlap - save current interval and start a new one
                mergedIntervals.add(currentInterval)
                currentInterval = event.startTime to event.endTime
            }
        }
        // Don't forget to add the last interval
        mergedIntervals.add(currentInterval)
        
        // Sum durations of all merged intervals in hours
        return mergedIntervals.sumOf { (start, end) ->
            ChronoUnit.MINUTES.between(start, end).toDouble() / 60.0
        }
    }
    
    /**
     * Get the end time for a given time horizon
     * 
     * @param timeHorizon The time horizon
     * @param now Reference time
     * @return End time of the horizon
     */
    private fun getTimeHorizonEnd(timeHorizon: TimeHorizon, now: LocalDateTime): LocalDateTime {
        return when (timeHorizon) {
            TimeHorizon.Today -> {
                // End of today (23:59:59.999)
                now.with(LocalTime.MAX)
            }
            TimeHorizon.ThisWeek -> {
                // End of Sunday (last day of week)
                val daysUntilSunday = 7 - now.dayOfWeek.value
                now.plusDays(daysUntilSunday.toLong()).with(LocalTime.MAX)
            }
            TimeHorizon.ThisMonth -> {
                // End of current month
                val lastDayOfMonth = now.toLocalDate().withDayOfMonth(1)
                    .plusMonths(1)
                    .minusDays(1)
                LocalDateTime.of(lastDayOfMonth, LocalTime.MAX)
            }
            TimeHorizon.ThisYear -> {
                // End of current year (Dec 31, 23:59:59.999)
                val endOfYear = now.toLocalDate().withMonth(12).withDayOfMonth(31)
                LocalDateTime.of(endOfYear, LocalTime.MAX)
            }
            TimeHorizon.ThisLife -> {
                // For "This Life", use a far future date (e.g., 50 years from now)
                now.plusYears(50).with(LocalTime.MAX)
            }
        }
    }
    
    /**
     * Helper function to get max of two LocalDateTime values
     */
    private fun maxOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a.isAfter(b)) a else b
    }
    
    /**
     * Helper function to get min of two LocalDateTime values
     */
    private fun minOf(a: LocalDateTime, b: LocalDateTime): LocalDateTime {
        return if (a.isBefore(b)) a else b
    }
}
