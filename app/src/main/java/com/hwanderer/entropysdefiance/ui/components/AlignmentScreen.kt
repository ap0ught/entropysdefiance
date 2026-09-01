// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * A full-screen dialog with a strong dimming scrim, used for the alignment screen's sub-dialogs.
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
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismissRequest),
                color = Color.Black.copy(alpha = 0.9f)
            ) {}

            Box(modifier = Modifier.fillMaxWidth(0.9f)) {
                content()
            }
        }
    }
}

/**
 * A full screen component for the Alignment mode workflow.
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
        onDeleteTask(id)
        onAddTask(title, horizon)
    },
    modifier: Modifier = Modifier
) {
    var showTaskDialog by remember { mutableStateOf(false) }
    var taskTitle by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var currentEditTaskId by remember { mutableStateOf("") }
    var isDeadlineTask by remember { mutableStateOf(false) }
    var selectedDeadline by remember { mutableStateOf<LocalDate?>(null) }
    var daysNeeded by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val guidanceMessage = when (currentHorizon) {
        TimeHorizon.ThisLife -> "Consider your life goals. What do you want to accomplish?"
        TimeHorizon.ThisYear -> "What are your goals for this year that align with your life vision?"
        TimeHorizon.ThisMonth -> "What monthly objectives will move you toward your yearly goals?"
        TimeHorizon.ThisWeek -> "What can you accomplish this week to progress on your monthly objectives?"
        TimeHorizon.Today -> "What specific tasks can you do today to advance your weekly goals?"
    }

    val accent = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = onBg.copy(alpha = 0.5f)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Transparent header: a small ALIGNMENT label over the horizon name in the accent.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(safeTopInsets())
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Alignment", tint = onBg)
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "ALIGNMENT",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                        color = muted
                    )
                    Text(
                        text = currentHorizon.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.W100,
                        color = accent
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
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
                .padding(horizontal = 24.dp)
        ) {
            // Guidance — borderless, centered, quiet.
            Text(
                text = guidanceMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )

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
                        text = "Nothing here yet.\nAdd what belongs to ${currentHorizon.title.lowercase()}.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = muted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    // Clears the add/next FABs, which float over the list rather than
                    // reserving space in the Scaffold.
                    contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
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

    // Add / edit task dialog
    if (showTaskDialog) {
        val editingTask = tasks.find { it.id == currentEditTaskId }
        val canConfirm = taskTitle.isNotBlank() && (
            isEditMode || !isDeadlineTask ||
                (selectedDeadline != null && (daysNeeded.toIntOrNull() ?: 0) > 0)
            )

        fun confirm() {
            if (!canConfirm) return
            if (isEditMode) {
                onUpdateTask(currentEditTaskId, taskTitle, currentHorizon)
            } else if (isDeadlineTask) {
                onAddDeadlineTask(taskTitle, selectedDeadline!!, daysNeeded.toInt())
            } else {
                onAddTask(taskTitle, currentHorizon)
            }
            taskTitle = ""
            isDeadlineTask = false
            selectedDeadline = null
            daysNeeded = ""
            showTaskDialog = false
        }

        StrongDimDialog(onDismissRequest = { showTaskDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                val dialogOnSurface = MaterialTheme.colorScheme.onSurface
                val dialogMuted = dialogOnSurface.copy(alpha = 0.4f)
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Edit task" else "New task",
                        style = MaterialTheme.typography.titleMedium,
                        color = accent
                    )

                    UnderlinedInput(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        placeholder = "Task",
                        accent = accent,
                        textColor = dialogOnSurface,
                        placeholderColor = dialogMuted,
                        textStyle = MaterialTheme.typography.titleLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.W100
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { confirm() })
                    )

                    if (!isEditMode) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModeLabel(
                                text = "Time Horizon",
                                selected = !isDeadlineTask,
                                accent = accent,
                                mutedColor = dialogMuted,
                                onClick = { isDeadlineTask = false }
                            )
                            ModeLabel(
                                text = "Deadline",
                                selected = isDeadlineTask,
                                accent = accent,
                                mutedColor = dialogMuted,
                                onClick = { isDeadlineTask = true }
                            )
                        }

                        if (isDeadlineTask) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedDeadline
                                        ?.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                                        ?: "Select deadline",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedDeadline != null) dialogOnSurface else dialogMuted,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDatePicker = true }
                                        .padding(vertical = 4.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(accent.copy(alpha = 0.6f))
                                )
                            }

                            UnderlinedInput(
                                value = daysNeeded,
                                onValueChange = { new -> daysNeeded = new.filter { it.isDigit() }.take(4) },
                                placeholder = "Days needed",
                                accent = accent,
                                textColor = dialogOnSurface,
                                placeholderColor = dialogMuted,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.W100
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { confirm() })
                            )
                        }
                    } else if (editingTask?.isDeadlineTask == true) {
                        Text(
                            text = "Deadline: ${editingTask.deadline?.format(DateTimeFormatter.ISO_LOCAL_DATE) ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = dialogMuted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showTaskDialog = false }) {
                            Text("Cancel", color = dialogMuted)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { confirm() }, enabled = canConfirm) {
                            Text(
                                text = if (isEditMode) "Update" else "Add",
                                color = if (canConfirm) accent else dialogMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
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
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                ) {
                    val scrollState = rememberScrollState()
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
    val onBg = MaterialTheme.colorScheme.onBackground

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (task.isDeadlineTask && task.deadline != null) {
                    "${task.deadline.format(DateTimeFormatter.ofPattern("dd.MM."))}  ·  ${task.title}"
                } else {
                    task.title
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W100,
                color = onBg,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 8.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!task.isDeadlineTask) {
                IconButton(onClick = { showMoveDialog = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Move Task",
                        tint = onBg.copy(alpha = 0.45f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Delete Task",
                    tint = onBg.copy(alpha = 0.45f)
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = onBg.copy(alpha = 0.12f))
    }

    if (showMoveDialog) {
        StrongDimDialog(onDismissRequest = { showMoveDialog = false }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Move to",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp),
                    color = onBg.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TimeHorizon.entries.forEach { horizon ->
                    if (horizon != currentHorizon) {
                        Text(
                            text = horizon.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                            fontWeight = FontWeight.W100,
                            color = onBg,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable {
                                    onMove(horizon)
                                    showMoveDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 24.dp)
                        )
                    }
                }
            }
        }
    }
}
