package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay

@Composable
fun TimeHorizonProgressBar(
    timeHorizon: TimeHorizon,
    modifier: Modifier = Modifier
) {
    var progress by remember { mutableStateOf(0f) }
    
    // Update progress every minute
    LaunchedEffect(timeHorizon) {
        while (true) {
            progress = calculateProgress(timeHorizon)
            delay(60000) // Update every minute
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small
                )
        )
    }
}

private fun calculateProgress(timeHorizon: TimeHorizon): Float {
    val now = LocalDateTime.now()
    
    return when (timeHorizon) {
        TimeHorizon.Today -> {
            val startOfDay = now.with(LocalTime.MIN)
            val endOfDay = now.with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfDay, endOfDay)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfDay, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }
        TimeHorizon.ThisWeek -> {
            val startOfWeek = now.with(LocalTime.MIN).minusDays(now.dayOfWeek.value.toLong() - 1)
            val endOfWeek = startOfWeek.plusDays(6).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfWeek, endOfWeek)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfWeek, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }
        TimeHorizon.ThisMonth -> {
            val startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN)
            val endOfMonth = startOfMonth.plusMonths(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfMonth, endOfMonth)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfMonth, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }
        TimeHorizon.ThisYear -> {
            val startOfYear = now.withMonth(1).withDayOfMonth(1).with(LocalTime.MIN)
            val endOfYear = startOfYear.plusYears(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfYear, endOfYear)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfYear, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }
        TimeHorizon.ThisLife -> {
            // For life, we'll use a fixed age of 100 years as the total duration
            val birthDate = now.minusYears(30) // Assuming 30 years old as a default
            val deathDate = birthDate.plusYears(100)
            val totalMinutes = ChronoUnit.MINUTES.between(birthDate, deathDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(birthDate, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
        }
    }
} 