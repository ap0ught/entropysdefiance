package com.example.entropysdefiance.model

/**
 * Represents the calculated free time metric for a given time horizon
 */
data class FreeTimeMetric(
    val totalHoursRemaining: Double,      // Hours from now to horizon end (24h basis)
    val scheduledHours: Double,           // Non-overlapping event hours from now to end
    val freeHours24h: Double,             // totalHours - scheduledHours (24h basis)
    val productiveHoursAvailable: Double  // freeHours24h × productivityRatio
)
