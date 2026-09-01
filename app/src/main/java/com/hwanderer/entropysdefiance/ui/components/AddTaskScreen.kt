// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.hwanderer.entropysdefiance.model.TimeHorizon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onAddTask: (String, TimeHorizon) -> Unit,
    onAddDeadlineTask: (String, LocalDate, Int) -> Unit = { _, _, _ -> },
    onDismiss: () -> Unit
) {
    var taskTitle by remember { mutableStateOf("") }
    var selectedTimeHorizon by remember { mutableStateOf(TimeHorizon.Today) }
    var isDeadlineTask by remember { mutableStateOf(false) }
    var selectedDeadline by remember { mutableStateOf<LocalDate?>(null) }
    var daysNeeded by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val canSubmit = taskTitle.isNotBlank() && (
        !isDeadlineTask ||
            (selectedDeadline != null && (daysNeeded.toIntOrNull() ?: 0) > 0)
        )

    fun submitTask() {
        if (!canSubmit) return
        if (isDeadlineTask) {
            onAddDeadlineTask(taskTitle, selectedDeadline!!, daysNeeded.toInt())
        } else {
            onAddTask(taskTitle, selectedTimeHorizon)
        }
        onDismiss()
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val accent = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = onBg.copy(alpha = 0.4f)

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = onBg
                        )
                    }
                },
                title = { },
                actions = {
                    TextButton(
                        onClick = { submitTask() },
                        enabled = canSubmit
                    ) {
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (canSubmit) accent else muted
                        )
                    }
                },
                // Material's default here is `statusBars`, which collapses to zero in
                // fullscreen mode and puts the bar under the camera cutout.
                windowInsets = safeTopInsets(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Task title — a borderless, centered input with a single accent underline.
            UnderlinedInput(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                placeholder = "What needs doing?",
                accent = accent,
                textColor = onBg,
                placeholderColor = muted,
                modifier = Modifier.focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.headlineSmall.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.W100
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitTask() })
            )

            // Mode toggle — plain text, active one in the accent with a short underline.
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeLabel(
                    text = "Time Horizon",
                    selected = !isDeadlineTask,
                    accent = accent,
                    mutedColor = muted,
                    onClick = { isDeadlineTask = false }
                )
                ModeLabel(
                    text = "Deadline",
                    selected = isDeadlineTask,
                    accent = accent,
                    mutedColor = muted,
                    onClick = { isDeadlineTask = true }
                )
            }

            if (isDeadlineTask) {
                // Deadline date — a tappable, centered field with the same underline treatment.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = selectedDeadline
                            ?.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
                            ?: "Select deadline",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.W100,
                        color = if (selectedDeadline != null) onBg else muted,
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
                    textColor = onBg,
                    placeholderColor = muted,
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.W100
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submitTask() })
                )
            } else {
                // Horizon picker — the five scales as a centered vertical list of large text.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeHorizon.entries.forEach { horizon ->
                        SelectableTextRow(
                            text = horizon.title,
                            selected = selectedTimeHorizon == horizon,
                            onClick = { selectedTimeHorizon = horizon }
                        )
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

        val scrollState = androidx.compose.foundation.rememberScrollState()

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showDatePicker = false },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showDatePicker = false },
                    color = Color.Black.copy(alpha = 0.9f)
                ) {}

                Surface(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                    ) {
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
}

