package com.example.entropysdefiance.utils

import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Calculate the appropriate TimeHorizon for a given date.
 * The date should fall into the first applicable horizon (from smallest to largest).
 */
fun calculateTimeHorizonFromDate(date: LocalDate): TimeHorizon {
    val now = LocalDate.now()
    
    // Check from smallest to largest horizon
    // Today
    if (date == now) {
        return TimeHorizon.Today
    }
    
    // This Week
    val startOfWeek = now.minusDays((now.dayOfWeek.value - 1).toLong())
    val endOfWeek = startOfWeek.plusDays(6)
    if (date.isAfter(startOfWeek.minusDays(1)) && date.isBefore(endOfWeek.plusDays(1))) {
        return TimeHorizon.ThisWeek
    }
    
    // This Month
    val startOfMonth = now.withDayOfMonth(1)
    val endOfMonth = startOfMonth.plusMonths(1).minusDays(1)
    if (date.isAfter(startOfMonth.minusDays(1)) && date.isBefore(endOfMonth.plusDays(1))) {
        return TimeHorizon.ThisMonth
    }
    
    // This Year
    val startOfYear = now.withMonth(1).withDayOfMonth(1)
    val endOfYear = startOfYear.plusYears(1).minusDays(1)
    if (date.isAfter(startOfYear.minusDays(1)) && date.isBefore(endOfYear.plusDays(1))) {
        return TimeHorizon.ThisYear
    }
    
    // This Life (fallback)
    return TimeHorizon.ThisLife
}

