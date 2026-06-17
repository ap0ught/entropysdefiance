package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwanderer.entropysdefiance.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val taskViewModel: TaskViewModel = viewModel()

    var totalTasks by remember { mutableStateOf(0) }
    var completedTasks by remember { mutableStateOf(0) }
    var pendingTasks by remember { mutableStateOf(0) }
    var oldestTaskDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            totalTasks = taskViewModel.getTotalTaskCount()
            completedTasks = taskViewModel.getCompletedTaskCount()
            pendingTasks = taskViewModel.getPendingTaskCount()
            oldestTaskDate = taskViewModel.getOldestTaskDate()
            isLoading = false
        }
    }

    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100) else 0f
    val oldestTaskAge = oldestTaskDate?.let {
        ChronoUnit.DAYS.between(it, LocalDateTime.now())
    } ?: 0L

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            ScreenHeader(title = "Statistics", onBack = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Task overview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    StatisticItem("Total tasks created", totalTasks.toString())
                    StatisticItem("Completed", completedTasks.toString())
                    StatisticItem("Pending", pendingTasks.toString())
                    StatisticItem("Completion rate", "${completionRate.toInt()}%")
                    StatisticItem("Oldest task age", "$oldestTaskAge days")
                }
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}
