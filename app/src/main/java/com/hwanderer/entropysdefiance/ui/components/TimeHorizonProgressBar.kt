// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import android.util.Log

@Composable
fun TimeHorizonProgressBar(
    timeHorizon: TimeHorizon,
    modifier: Modifier = Modifier,
    showPercentage: Boolean = false
) {
    // Calculate progress immediately for initial display
    val initialProgress = remember(timeHorizon) { calculateProgress(timeHorizon) }
    var progress by remember { mutableStateOf(initialProgress) }
    
    // Log the progress to verify calculation is working
    LaunchedEffect(initialProgress) {
        Log.d("TimeHorizonProgressBar", "Initial progress: $initialProgress for $timeHorizon")
    }
    
    // Update progress every minute
    LaunchedEffect(timeHorizon) {
        while (true) {
            progress = calculateProgress(timeHorizon)
            Log.d("TimeHorizonProgressBar", "Updated progress: $progress for $timeHorizon")
            delay(60000) // Update every minute
        }
    }

    // Box for the progress bar with more padding for visibility
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Use a more visible background color (light gray that will show on both light/dark backgrounds)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp) // Slightly thicker bar
                .clip(MaterialTheme.shapes.small)
                .background(Color.White.copy(alpha = 0.4f)) // Higher opacity for better visibility
        ) {
            // Use a brighter color for the progress that will stand out against the dark overlay
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .clip(MaterialTheme.shapes.small)
                    .background(Color(0xFFE1FF00)) // Bright lime color that will be visible against dark background
            )
        }
        
        // Only show percentage if requested
        if (showPercentage) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White, // Pure white for maximum contrast
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun calculateProgress(timeHorizon: TimeHorizon): Float {
    val now = LocalDateTime.now()
    
    // Ensure we have a non-zero progress to make bar visible for testing
    val calculatedProgress = when (timeHorizon) {
        TimeHorizon.Today -> {
            val startOfDay = now.with(LocalTime.MIN)
            val endOfDay = now.with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfDay, endOfDay)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfDay, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisWeek -> {
            val startOfWeek = now.with(LocalTime.MIN).minusDays(now.dayOfWeek.value.toLong() - 1)
            val endOfWeek = startOfWeek.plusDays(6).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfWeek, endOfWeek)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfWeek, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisMonth -> {
            val startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN)
            val endOfMonth = startOfMonth.plusMonths(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfMonth, endOfMonth)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfMonth, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisYear -> {
            val startOfYear = now.withMonth(1).withDayOfMonth(1).with(LocalTime.MIN)
            val endOfYear = startOfYear.plusYears(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfYear, endOfYear)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfYear, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisLife -> {
            // For life, we'll use a fixed age of 100 years as the total duration
            val birthDate = now.minusYears(30) // Assuming 30 years old as a default
            val deathDate = birthDate.plusYears(100)
            val totalMinutes = ChronoUnit.MINUTES.between(birthDate, deathDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(birthDate, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
    }
    
    // Ensure bar is always visible (even if 0% progress)
    return calculatedProgress.coerceAtLeast(0.01f)
} 