package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon

@Composable
fun TimeHorizonTabs(
    selectedTimeHorizon: TimeHorizon,
    onTimeHorizonSelected: (TimeHorizon) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTimeHorizon.ordinal,
        edgePadding = 0.dp
    ) {
        TimeHorizon.values().forEach { timeHorizon ->
            Tab(
                selected = timeHorizon == selectedTimeHorizon,
                onClick = { onTimeHorizonSelected(timeHorizon) },
                text = { Text(timeHorizon.title) }
            )
        }
    }
}

@Composable
fun TaskList(
    timeHorizon: TimeHorizon,
    tasks: List<Task>,
    onAddTask: (String, TimeHorizon) -> Unit,
    onCompleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks.filter { it.timeHorizon == timeHorizon }, key = { it.id }) { task ->
                TaskItem(
                    task = task,
                    onComplete = { onCompleteTask(task.id) }
                )
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            timeHorizon = timeHorizon,
            onAddTask = onAddTask,
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = task.title)
            IconButton(onClick = onComplete) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Complete Task")
            }
        }
    }
}

@Composable
private fun AddTaskDialog(
    timeHorizon: TimeHorizon,
    onAddTask: (String, TimeHorizon) -> Unit,
    onDismiss: () -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task to ${timeHorizon.title}") },
        text = {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                label = { Text("Task") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (taskTitle.isNotBlank()) {
                        onAddTask(taskTitle, timeHorizon)
                        onDismiss()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 