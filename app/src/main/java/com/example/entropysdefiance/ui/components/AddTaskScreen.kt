package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import com.example.entropysdefiance.model.TimeHorizon
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    fun submitTask() {
        if (taskTitle.isNotBlank()) {
            if (isDeadlineTask) {
                val deadline = selectedDeadline
                val days = daysNeeded.toIntOrNull()
                if (deadline != null && days != null && days > 0) {
                    onAddDeadlineTask(taskTitle, deadline, days)
                    onDismiss()
                }
            } else {
                onAddTask(taskTitle, selectedTimeHorizon)
                onDismiss()
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                },
                title = { },
                actions = {
                    Button(
                        onClick = { submitTask() },
                        enabled = taskTitle.isNotBlank() && (!isDeadlineTask || (selectedDeadline != null && daysNeeded.toIntOrNull() != null && daysNeeded.toIntOrNull()!! > 0)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Add")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = taskTitle,
                onValueChange = { taskTitle = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text("Enter note") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        submitTask()
                    }
                ),
                singleLine = true
            )

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Time horizon selection in 2-column grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Time Horizon",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    
                    // Two-column grid layout
                    val horizons = TimeHorizon.values().toList()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Display horizons in rows of 2
                        horizons.chunked(2).forEach { rowHorizons: List<TimeHorizon> ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (rowHorizons.size == 1) Arrangement.Center else Arrangement.spacedBy(8.dp)
                            ) {
                                rowHorizons.forEach { horizon: TimeHorizon ->
                                    Surface(
                                        modifier = Modifier
                                            .then(
                                                if (rowHorizons.size == 1) {
                                                    Modifier.wrapContentWidth()
                                                } else {
                                                    Modifier.weight(1f)
                                                }
                                            )
                                            .clickable { selectedTimeHorizon = horizon },
                                        color = MaterialTheme.colorScheme.background
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp, horizontal = 20.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedTimeHorizon == horizon,
                                                onClick = { selectedTimeHorizon = horizon },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = MaterialTheme.colorScheme.primary
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = horizon.title,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
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
        
        val scrollState = androidx.compose.foundation.rememberScrollState()
        
        // Custom dialog with dark background to hide underlying content
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
                // Very dark scrim to hide background
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showDatePicker = false },
                    color = Color.Black.copy(alpha = 0.9f)
                ) {}
                
                // Date picker content
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