package com.example.entropysdefiance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.entropysdefiance.ui.theme.EntropysDefianceTheme
import com.example.entropysdefiance.model.TimeHorizon
import com.example.entropysdefiance.ui.components.TaskList
import com.example.entropysdefiance.ui.components.AddTaskScreen
import com.example.entropysdefiance.ui.components.AppDrawer
import com.example.entropysdefiance.ui.components.CalendarAgendaWidget
import com.example.entropysdefiance.ui.components.CalendarSelectionDialog
import com.example.entropysdefiance.ui.components.SettingsDialog
import com.example.entropysdefiance.viewmodel.TaskViewModel
import com.example.entropysdefiance.viewmodel.LauncherViewModel
import com.example.entropysdefiance.viewmodel.SettingsViewModel
import com.example.entropysdefiance.data.CalendarEventProvider
import com.example.entropysdefiance.model.CalendarEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import android.util.Log
import com.example.entropysdefiance.viewmodel.CalendarViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        WindowCompat.getInsetsController(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        
        setContent {
            EntropysDefianceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create all required ViewModels
                    val taskViewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                    val launcherViewModel: LauncherViewModel = viewModel(factory = viewModelFactory)
                    val calendarViewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
                    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                    
                    // Use the TaskApp composable with Calendar integration
                    TaskApp(
                        taskViewModel = taskViewModel,
                        launcherViewModel = launcherViewModel,
                        calendarViewModel = calendarViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TaskApp(
    taskViewModel: TaskViewModel,
    launcherViewModel: LauncherViewModel,
    calendarViewModel: CalendarViewModel,
    settingsViewModel: SettingsViewModel
) {
    var showAddScreen by remember { mutableStateOf(false) }
    val tasks by taskViewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val apps by launcherViewModel.apps.collectAsStateWithLifecycle(initialValue = emptyList())
    var isPeeking by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    
    // Get calendar events from the ViewModel
    val calendarEvents by calendarViewModel.events.collectAsState()
    val isCalendarLoading by calendarViewModel.isLoading.collectAsState()
    
    // Use a simpler approach for hasCalendarPermission
    var hasCalendarPermission by remember { mutableStateOf(false) }
    
    // Initialize calendar events when the app starts
    LaunchedEffect(Unit) {
        calendarViewModel.initialize()
        // Just use a default value for now, and update it later if needed
        hasCalendarPermission = true
    }
    
    // Calendar selection related state
    val availableCalendars by calendarViewModel.availableCalendars.collectAsState()
    
    // Settings related state
    val appSettings by settingsViewModel.settings.collectAsState()
    val showSettingsDialog by settingsViewModel.showSettingsDialog.collectAsState()
    val showCalendarSelection by settingsViewModel.showCalendarSelection.collectAsState()
    
    // Define a proper coroutine scope
    val coroutineScope = rememberCoroutineScope()
    
    // Define our bottom sheet state for the app drawer
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentTimeHorizon = remember(tasks) {
        TimeHorizon.entries.firstOrNull { horizon ->
            tasks.any { it.timeHorizon == horizon }
        } ?: TimeHorizon.Today
    }

    val nextTimeHorizon = remember(currentTimeHorizon) {
        val horizons = TimeHorizon.entries
        val nextIndex = (currentTimeHorizon.ordinal + 1).coerceAtMost(horizons.size - 1)
        horizons[nextIndex]
    }

    val displayedTimeHorizon = if (isPeeking) nextTimeHorizon else currentTimeHorizon

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp.value
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent dark overlay that extends behind system bars
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.4f) // Slightly darker for better dimming
        ) {}

        if (showAddScreen) {
            // Show the AddTaskScreen on top of the overlay
            AddTaskScreen(
                onAddTask = { title, timeHorizon ->
                    taskViewModel.addTask(title, timeHorizon)
                    showAddScreen = false
                },
                onDismiss = { showAddScreen = false }
            )
        } else if (showAppDrawer) {
            // Use ModalBottomSheet with proper configuration for full-screen display
            ModalBottomSheet(
                onDismissRequest = { showAppDrawer = false },
                sheetState = bottomSheetState,
                dragHandle = { /* No visible drag handle */ },
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(),
                windowInsets = WindowInsets(0, 0, 0, 0) // Remove any insets to ensure full coverage
            ) {
                AppDrawer(
                    modifier = Modifier.fillMaxSize(),
                    apps = apps,
                    onAppClick = { app ->
                        launcherViewModel.launchApp(app.packageName, app.launchIntent)
                        showAppDrawer = false
                    },
                    onNotificationPullDown = {
                        // Handle notification pull down
                        coroutineScope.launch {
                            showAppDrawer = false
                            // Add additional actions for notification panel if needed
                        }
                    }
                )
            }
        } else if (showSettingsDialog) {
            // Show settings as a full screen replacement for main content
            SettingsDialog(
                appSettings = appSettings,
                onDismiss = { settingsViewModel.hideSettingsDialog() },
                onToggleCalendarWidget = { settingsViewModel.toggleCalendarWidget() },
                onCalendarDaysChange = { days -> 
                    settingsViewModel.updateCalendarDaysToShow(days)
                    // Reload calendar events with new days setting
                    calendarViewModel.loadEvents(days)
                },
                onToggleDarkTheme = { settingsViewModel.toggleDarkTheme() },
                onToggleDoubleTapDialer = { settingsViewModel.toggleDoubleTapForDialer() },
                onCalendarSelectionClick = { settingsViewModel.showCalendarSelection() }
            )
        } else if (showCalendarSelection) {
            // Show calendar selection as a full screen replacement for main content
            CalendarSelectionDialog(
                availableCalendars = availableCalendars,
                onCalendarToggle = { calendarId ->
                    calendarViewModel.toggleCalendarSelection(calendarId)
                },
                onDismiss = {
                    settingsViewModel.hideCalendarSelection()
                },
                onSelectAll = {
                    availableCalendars.forEach { calendar ->
                        if (!calendar.selected) {
                            calendarViewModel.toggleCalendarSelection(calendar.id)
                        }
                    }
                },
                onSelectNone = {
                    availableCalendars.forEach { calendar ->
                        if (calendar.selected) {
                            calendarViewModel.toggleCalendarSelection(calendar.id)
                        }
                    }
                }
            )
        } else {
            // Home screen content
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = { 
                            Text(
                                text = displayedTimeHorizon.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp)
                                    .clickable { 
                                        launcherViewModel.launchCalendarApp("org.fossify.calendar")
                                    },
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                )
                            )
                        },
                        modifier = Modifier.padding(top = 12.dp),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                },
                floatingActionButton = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { }, 
                            modifier = Modifier
                                .pointerInteropFilter { event ->
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            isPeeking = true
                                            true
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            isPeeking = false
                                            true
                                        }
                                        else -> false
                                    }
                                }
                                .size(48.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Peek Next Time Horizon",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        FloatingActionButton(
                            onClick = { showAddScreen = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        // Add gesture detection to the main screen
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    // Launch dialer app on double tap if enabled
                                    if (appSettings.enableDoubleTapForDialer) {
                                        launcherViewModel.launchDialerApp()
                                    }
                                },
                                onLongPress = {
                                    // Show settings on long press
                                    settingsViewModel.showSettingsDialog()
                                }
                            )
                        }
                ) {
                    // Create a Column to arrange our UI elements vertically
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Calculate the filtered tasks for the current time horizon
                        val currentTasks = remember(tasks, displayedTimeHorizon) {
                            tasks.filter { it.timeHorizon == displayedTimeHorizon }
                        }
                        
                        // Get screen height for calculations
                        val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                        
                        // Calculate the task list height based on number of tasks
                        // Give each task ~76dp (card + padding) and limit to 1/2 of screen height
                        val taskCount = currentTasks.size
                        val estimatedTaskHeight = 76.dp * taskCount + 48.dp // Add padding
                        val maxTaskListHeight = screenHeight * 0.45f // Reduced to 45% to ensure space for whitespace
                        val taskListHeight = estimatedTaskHeight.coerceAtMost(maxTaskListHeight)
                                    
                        // TaskList with dynamic height based on content
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(taskListHeight)
                        ) {
                            TaskList(
                                modifier = Modifier.fillMaxSize(),
                                timeHorizon = displayedTimeHorizon,
                                tasks = tasks,
                                onDeleteTask = taskViewModel::deleteTask,
                                onMoveTask = taskViewModel::moveTask
                            )
                        }
                        
                        // Add mandatory spacing between task list and calendar events
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Calendar section - only show if enabled in settings
                        if (appSettings.showCalendarWidget) {
                            // The calendar section takes remaining space with its own scrolling
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f) // Take remaining space
                            ) {
                                // Calendar widget with scrolling
                                if (hasCalendarPermission) {
                                    if (isCalendarLoading) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    } else {
                                        CalendarAgendaWidget(
                                            events = calendarEvents,
                                            onEventClick = { event ->
                                                // Handle calendar event click
                                                Toast.makeText(
                                                    context, 
                                                    "Opening event: ${event.title}", 
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                
                                                // Log event details
                                                Log.d("CalendarEvent", "Clicked on: ${event.title}")
                                                
                                                // Optionally launch the calendar app
                                                launcherViewModel.launchCalendarApp("org.fossify.calendar")
                                            },
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(bottom = 80.dp) // Increased padding to avoid peek button
                                        )
                                    }
                                } else {
                                    // If no permission, use the mock data provider
                                    val mockEvents = remember { 
                                        CalendarEventProvider.getUpcomingEvents() 
                                    }
                                    
                                    CalendarAgendaWidget(
                                        events = mockEvents,
                                        onEventClick = { event ->
                                            // Handle calendar event click
                                            Toast.makeText(
                                                context, 
                                                "Opening event: ${event.title}", 
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            
                                            // Request calendar permission
                                            calendarViewModel.initialize()
                                        },
                                        modifier = Modifier.padding(bottom = 80.dp) // Increased padding to avoid peek button
                                    )
                                }
                            }
                        }
                    }
                    
                    // App drawer overlay - move to bottom area of the screen and improve gesture detection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp) // Increased height for better detection area (was 60.dp)
                            .align(Alignment.BottomCenter)
                            .pointerInput(Unit) {
                                var totalDragDistance = 0f
                                
                                detectVerticalDragGestures(
                                    onDragStart = { 
                                        dragStart = it 
                                        totalDragDistance = 0f
                                    },
                                    onDragEnd = { 
                                        // If we reached a smaller threshold on drag end, also trigger
                                        if (totalDragDistance < -20f) {
                                            showAppDrawer = true
                                        }
                                    },
                                    onDragCancel = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        // Track total distance dragged
                                        totalDragDistance += dragAmount
                                        
                                        // Use a lower threshold for more responsive detection
                                        // Only respond to upward swipes (negative dragAmount)
                                        if (dragAmount < -5f) {
                                            change.consume()
                                            
                                            // If significant upward movement, show app drawer immediately
                                            if (totalDragDistance < -30f) {
                                                showAppDrawer = true
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }
    }
}