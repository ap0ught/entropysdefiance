package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwanderer.entropysdefiance.model.TimeHorizon
import com.hwanderer.entropysdefiance.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskViewModel: TaskViewModel = viewModel()
    
    var totalTasks by remember { mutableStateOf(0) }
    var completedTasks by remember { mutableStateOf(0) }
    var pendingTasks by remember { mutableStateOf(0) }
    var oldestTaskDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var tasksByHorizon by remember { mutableStateOf(mapOf<TimeHorizon, Int>()) }
    var completedTasksByHorizon by remember { mutableStateOf(mapOf<TimeHorizon, Int>()) }
    var pendingTasksByHorizon by remember { mutableStateOf(mapOf<TimeHorizon, Int>()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        scope.launch {
            totalTasks = taskViewModel.getTotalTaskCount()
            completedTasks = taskViewModel.getCompletedTaskCount()
            pendingTasks = taskViewModel.getPendingTaskCount()
            oldestTaskDate = taskViewModel.getOldestTaskDate()
            
            // Note: We don't have tasks by horizon statistics yet in the new file-based implementation
            // This would require reading and processing the full task list
            isLoading = false
        }
    }
    
    val completionRate = if (totalTasks > 0) (completedTasks.toFloat() / totalTasks * 100) else 0f
    val oldestTaskAge = oldestTaskDate?.let { 
        ChronoUnit.DAYS.between(it, LocalDateTime.now()) 
    } ?: 0L
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Task Overview",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            StatisticItem("Total Tasks Created", totalTasks.toString())
                            StatisticItem("Completed Tasks", completedTasks.toString())
                            StatisticItem("Pending Tasks", pendingTasks.toString())
                            StatisticItem("Completion Rate", "${completionRate.toInt()}%")
                            StatisticItem("Oldest Task Age", "${oldestTaskAge} days")
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Tasks by Time Horizon",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        TimeHorizon.values().forEach { horizon ->
                            val totalCount = tasksByHorizon[horizon] ?: 0
                            val completedCount = completedTasksByHorizon[horizon] ?: 0
                            val pendingCount = pendingTasksByHorizon[horizon] ?: 0
                            
                            if (totalCount > 0) {
                                StatisticItem(
                                    "${horizon.title} (Total)", 
                                    totalCount.toString()
                                )
                                StatisticItem(
                                    "  - Completed", 
                                    completedCount.toString(),
                                    Color.Green
                                )
                                StatisticItem(
                                    "  - Pending", 
                                    pendingCount.toString(),
                                    Color(0xFFFF9800) // Orange color
                                )
                            }
                        }
                    }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
