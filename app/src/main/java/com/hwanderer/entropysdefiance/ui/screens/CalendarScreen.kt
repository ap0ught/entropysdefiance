package com.hwanderer.entropysdefiance.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hwanderer.entropysdefiance.model.CalendarEvent
import com.hwanderer.entropysdefiance.ui.components.CalendarAgendaWidget
import com.hwanderer.entropysdefiance.ui.components.PermissionRequest
import com.hwanderer.entropysdefiance.viewmodel.CalendarViewModel
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    onEventClick: (CalendarEvent) -> Unit = {},
    viewModel: CalendarViewModel = viewModel()
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasPermission by viewModel.hasCalendarPermission
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Handle permission request result
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            viewModel.updatePermissionState()
            if (!isGranted) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Calendar permission is required to show events"
                    )
                }
            }
        }
    )
    
    // Load events when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                !hasPermission -> {
                    // Show permission request
                    PermissionRequest(
                        title = "Calendar Permission Required",
                        rationale = "We need access to your calendar to display upcoming events.",
                        onRequestPermission = {
                            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                        },
                        settingsIntent = viewModel.getAppSettingsIntent()
                    )
                }
                
                isLoading -> {
                    // Show loading indicator
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                else -> {
                    // Show calendar widget with events
                    CalendarAgendaWidget(
                        events = events,
                        onEventClick = onEventClick
                    )
                }
            }
        }
    }
} 