package com.example.entropysdefiance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Custom dialog with strong dimming effect
 */
@Composable
private fun StrongDimDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Dark scrim that covers the entire screen
            // This is placed behind the dialog content
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismissRequest),
                color = Color.Black.copy(alpha = 0.9f) // Very dark dimming to hide background
            ) {}
            
            // Dialog content
            Box(modifier = Modifier.fillMaxWidth(0.9f)) {
                content()
            }
        }
    }
}

/**
 * A full screen component for the Alignment mode workflow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlignmentScreen(
    currentHorizon: TimeHorizon,
    tasks: List<Task>,
    onNext: () -> Unit,
    onClose: () -> Unit,
    onAddTask: (String, TimeHorizon) -> Unit,
    onAddDeadlineTask: (String, LocalDate, Int) -> Unit = { _, _, _ -> },
    onDeleteTask: (String) -> Unit,
    onMoveTask: (String, TimeHorizon) -> Unit,
    onUpdateTask: (String, String, TimeHorizon) -> Unit = { id, title, horizon -> 
        // Default implementation for backward compatibility
        onDeleteTask(id)
        onAddTask(title, horizon)
    },
    modifier: Modifier = Modifier
) {
    // State for add/edit task dialog
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var currentEditTaskId by remember { mutableStateOf("") }
    var isDeadlineTask by remember { mutableStateOf(false) }
    var selectedDeadline by remember { mutableStateOf<LocalDate?>(null) }
    var daysNeeded by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Create guidance message based on current horizon
    val guidanceMessage = when (currentHorizon) {
        TimeHorizon.ThisLife -> "Consider your life goals. What do you want to accomplish?"
        TimeHorizon.ThisYear -> "What are your goals for this year that align with your life vision?"
        TimeHorizon.ThisMonth -> "What monthly objectives will move you toward your yearly goals?"
        TimeHorizon.ThisWeek -> "What can you accomplish this week to progress on your monthly objectives?"
        TimeHorizon.Today -> "What specific tasks can you do today to advance your weekly goals?"
    }
    
    Scaffold(
        topBar = {
            // Custom top bar implementation for true centering
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 12.dp) // Increased top padding significantly
                ) {
                    // Left-aligned close button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close Alignment",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // Truly centered title - now with more top padding through the parent Box
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "ALIGNMENT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = currentHorizon.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add task button
                SmallFloatingActionButton(
                    onClick = { 
                        taskTitle = ""
                        isEditMode = false
                        isDeadlineTask = false
                        selectedDeadline = null
                        daysNeeded = ""
                        showTaskDialog = true 
                    },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Add Task")
                }
                
                // Next horizon button
                FloatingActionButton(
                    onClick = onNext,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Horizon")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Guidance text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = guidanceMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Tasks for current horizon
            val horizonTasks = remember(tasks, currentHorizon) {
                tasks.filter { it.timeHorizon == currentHorizon }
            }
            
            if (horizonTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks for ${currentHorizon.title}.\nAdd some to proceed with alignment.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(horizonTasks, key = { it.id }) { task ->
                        AlignmentTaskItem(
                            task = task,
                            onDelete = { onDeleteTask(task.id) },
                            onMove = { newHorizon -> onMoveTask(task.id, newHorizon) },
                            onEdit = { 
                                currentEditTaskId = task.id
                                taskTitle = task.title
                                isEditMode = true
                                isDeadlineTask = task.isDeadlineTask
                                selectedDeadline = task.deadline
                                daysNeeded = ""
                                showTaskDialog = true
                            },
                            currentHorizon = currentHorizon
                        )
                    }
                }
            }
        }
    }
    
    // Dialog for adding or editing a task
    if (showTaskDialog) {
        StrongDimDialog(onDismissRequest = { showTaskDialog = false }) {
            AlertDialog(
                onDismissRequest = { showTaskDialog = false },
                title = { Text(if (isEditMode) "Edit Task" else "Add Task") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = taskTitle,
                            onValueChange = { taskTitle = it },
                            label = { Text("Task") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        if (!isEditMode) {
                            // Task type selection - compact design using FilterChip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !isDeadlineTask,
                                    onClick = { isDeadlineTask = false },
                                    label = { Text("Time Horizon") },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = isDeadlineTask,
                                    onClick = { isDeadlineTask = true },
                                    label = { Text("Deadline") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            if (isDeadlineTask) {
                                // Deadline task fields
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = selectedDeadline?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "Select Deadline",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Icon(Icons.Default.Edit, contentDescription = "Select Date")
                                    }
                                }
                                
                                OutlinedTextField(
                                    value = daysNeeded,
                                    onValueChange = { daysNeeded = it },
                                    label = { Text("Days Needed") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        
                        if (isEditMode) {
                            val editingTask = tasks.find { it.id == currentEditTaskId }
                            if (editingTask?.isDeadlineTask == true) {
                                // Show deadline info for deadline tasks being edited
                                Text(
                                    text = "Deadline: ${editingTask.deadline?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (taskTitle.isNotBlank()) {
                                if (isEditMode) {
                                    // Use the update task method
                                    onUpdateTask(currentEditTaskId, taskTitle, currentHorizon)
                                } else {
                                    if (isDeadlineTask) {
                                        val deadline = selectedDeadline
                                        val days = daysNeeded.toIntOrNull()
                                        if (deadline != null && days != null && days > 0) {
                                            onAddDeadlineTask(taskTitle, deadline, days)
                                        }
                                    } else {
                                        // For add, just add new task
                                        onAddTask(taskTitle, currentHorizon)
                                    }
                                }
                                taskTitle = ""
                                isDeadlineTask = false
                                selectedDeadline = null
                                daysNeeded = ""
                                showTaskDialog = false
                            }
                        },
                        enabled = taskTitle.isNotBlank() && (!isDeadlineTask || (selectedDeadline != null && daysNeeded.toIntOrNull() != null && daysNeeded.toIntOrNull()!! > 0))
                    ) {
                        Text(if (isEditMode) "Update" else "Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTaskDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDeadline?.let {
                it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            },
            yearRange = (java.time.Year.now().value)..(java.time.Year.now().value + 10),
            initialDisplayedMonthMillis = null
        )
        
        StrongDimDialog(onDismissRequest = { showDatePicker = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.95f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                ) {
                    // Scrollable date picker
                    val scrollState = androidx.compose.foundation.rememberScrollState()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(scrollState)
                    ) {
                        androidx.compose.material3.DatePicker(state = datePickerState)
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    selectedDeadline = java.time.Instant.ofEpochMilli(millis)
                                        .atZone(java.time.ZoneId.systemDefault())
                                        .toLocalDate()
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlignmentTaskItem(
    task: Task,
    onDelete: () -> Unit,
    onMove: (TimeHorizon) -> Unit,
    onEdit: () -> Unit,
    currentHorizon: TimeHorizon,
    modifier: Modifier = Modifier
) {
    var showMoveDialog by remember { mutableStateOf(false) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit) // Make the entire row clickable for edit
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Task title with deadline if applicable
            Text(
                text = if (task.isDeadlineTask && task.deadline != null) {
                    "${task.deadline.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} | ${task.title}"
                } else {
                    task.title
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Move button - hidden for deadline tasks
                if (!task.isDeadlineTask) {
                    IconButton(
                        onClick = { showMoveDialog = true }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Move Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
    
    // Dialog for moving a task to another time horizon
    if (showMoveDialog) {
        StrongDimDialog(onDismissRequest = { showMoveDialog = false }) {
            AlertDialog(
                onDismissRequest = { showMoveDialog = false },
                title = { Text("Move Task") },
                text = {
                    Column {
                        Text(
                            "Move task to another time horizon:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        TimeHorizon.entries.forEach { horizon ->
                            if (horizon != currentHorizon) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                onMove(horizon)
                                                showMoveDialog = false
                                            }
                                            .padding(8.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = horizon.title,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMoveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
} 