package com.hwanderer.entropysdefiance

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.hwanderer.entropysdefiance.ui.theme.EntropysDefianceTheme
import com.hwanderer.entropysdefiance.model.TimeHorizon
import com.hwanderer.entropysdefiance.ui.components.TaskList
import com.hwanderer.entropysdefiance.ui.components.AddTaskScreen
import com.hwanderer.entropysdefiance.ui.components.AppDrawer
import com.hwanderer.entropysdefiance.ui.components.CalendarAgendaWidget
import com.hwanderer.entropysdefiance.ui.components.CalendarSelectionDialog
import com.hwanderer.entropysdefiance.ui.components.SettingsDialog
import com.hwanderer.entropysdefiance.ui.components.CalendarSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.AppearanceSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.FreeTimeSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.FileSyncSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.PermissionsSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.BackupSettingsScreen
import com.hwanderer.entropysdefiance.ui.components.TimeHorizonProgressBar
import com.hwanderer.entropysdefiance.viewmodel.TaskViewModel
import com.hwanderer.entropysdefiance.viewmodel.LauncherViewModel
import com.hwanderer.entropysdefiance.viewmodel.SettingsViewModel
import com.hwanderer.entropysdefiance.data.CalendarEventProvider
import com.hwanderer.entropysdefiance.model.CalendarEvent
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.hwanderer.entropysdefiance.data.BackupRepository
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.ui.layout.onGloballyPositioned
import android.util.Log
import com.hwanderer.entropysdefiance.viewmodel.CalendarViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.take
import androidx.core.app.ActivityCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.hwanderer.entropysdefiance.ui.theme.*
import com.hwanderer.entropysdefiance.ui.components.ColorPaletteSelectionDialog
import com.hwanderer.entropysdefiance.model.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import com.hwanderer.entropysdefiance.ui.components.AlignmentScreen
import com.hwanderer.entropysdefiance.ui.components.StatisticsScreen
import com.hwanderer.entropysdefiance.ui.components.ZenModeScreen
import com.hwanderer.entropysdefiance.ui.components.ZenModeDurationDialog
import com.hwanderer.entropysdefiance.viewmodel.AlignmentViewModel
import com.hwanderer.entropysdefiance.viewmodel.ZenModeViewModel
import com.hwanderer.entropysdefiance.viewmodel.TutorialViewModel
import com.hwanderer.entropysdefiance.ui.components.WelcomeScreen
import com.hwanderer.entropysdefiance.ui.components.StorageSetupScreen

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
            // Get settings from the ViewModel
            val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
            val appSettings by settingsViewModel.settings.collectAsState(AppSettings())
            
            // Get the selected color palette based on settings
            val selectedColorPalette = remember(appSettings.colorTheme) {
                availableColorPalettes.find { it.displayName == appSettings.colorTheme } ?: GreenPalette
            }
            
            EntropysDefianceTheme(
                colorPalette = selectedColorPalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Create all required ViewModels
                    val taskViewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                    val launcherViewModel: LauncherViewModel = viewModel(factory = viewModelFactory)
                    val calendarViewModel: CalendarViewModel = viewModel(factory = viewModelFactory)
                    val zenModeViewModel: ZenModeViewModel = viewModel(factory = viewModelFactory)
                    val tutorialViewModel: TutorialViewModel = viewModel(factory = viewModelFactory)

                    // Set TaskViewModel in ZenModeViewModel for task completion
                    LaunchedEffect(Unit) {
                        zenModeViewModel.setTaskViewModel(taskViewModel)
                    }

                    // Use the TaskApp composable with Calendar integration
                    TaskApp(
                        taskViewModel = taskViewModel,
                        launcherViewModel = launcherViewModel,
                        calendarViewModel = calendarViewModel,
                        settingsViewModel = settingsViewModel,
                        viewModelFactory = viewModelFactory,
                        zenModeViewModel = zenModeViewModel,
                        tutorialViewModel = tutorialViewModel
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
    settingsViewModel: SettingsViewModel,
    viewModelFactory: ViewModelProvider.AndroidViewModelFactory,
    zenModeViewModel: ZenModeViewModel,
    tutorialViewModel: TutorialViewModel
) {
    var showAddScreen by remember { mutableStateOf(false) }
    val tasks by taskViewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val apps by launcherViewModel.apps.collectAsStateWithLifecycle(initialValue = emptyList())
    var isPeeking by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    
    // FAB expanded state
    var isFabExpanded by remember { mutableStateOf(false) }
    
    // Create AlignmentViewModel
    val alignmentViewModel: AlignmentViewModel = viewModel()
    val isAlignmentActive by alignmentViewModel.isAlignmentActive.collectAsState()
    val currentAlignmentHorizon by alignmentViewModel.currentAlignmentHorizon.collectAsState()
    
    // Get ZenModeViewModel state
    val zenModeState by zenModeViewModel.zenModeState.collectAsState()
    val showZenDurationDialog by zenModeViewModel.showDurationDialog.collectAsState()
    val selectedTask by zenModeViewModel.selectedTask.collectAsState()
    
    // Get calendar events from the ViewModel
    val calendarEvents by calendarViewModel.events.collectAsState()
    val isCalendarLoading by calendarViewModel.isLoading.collectAsState()
    
    // Calendar permission state
    val hasCalendarPermission by calendarViewModel.hasCalendarPermission
    
    // Track whether settings have been loaded
    var settingsLoaded by remember { mutableStateOf(false) }
    
    // Backup repository
    val context = LocalContext.current
    
    // Permission launcher for calendar
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            calendarViewModel.updatePermissionState()
        }
    )
    

    val backupRepository = remember { BackupRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // File picker launchers for CSV export/import
    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        // Export current tasks to CSV
                        val settings = settingsViewModel.settings.first()
                        val filePath = settings.taskFilePath
                        if (filePath.isEmpty()) {
                            Toast.makeText(context, "No task file configured", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        
                        val fileRepo = com.hwanderer.entropysdefiance.data.TaskFileRepository(context)
                        val allTasks = fileRepo.readTasksFromFile(filePath)
                        
                        val parcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "w")
                        if (parcelFileDescriptor != null) {
                            backupRepository.exportTasksToCsv(allTasks, parcelFileDescriptor)
                            parcelFileDescriptor.close()
                            Toast.makeText(context, "Tasks exported successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
    
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            val importedTasks = backupRepository.importTasksFromCsv(inputStream)
                            inputStream.close()
                            
                            // Import tasks into the file-based system
                            val result = taskViewModel.importTasksFromCsv(importedTasks)
                            result.fold(
                                onSuccess = { count ->
                                    Toast.makeText(context, "Successfully imported $count tasks", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { error ->
                                    Toast.makeText(context, "Import failed: ${error.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
    
    // Folder picker for setting up file sync
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    // Take persistable URI permission for long-term access
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    
                    // Create tasks.json file in the selected folder
                    val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, uri)
                    if (docTree != null && docTree.canWrite()) {
                        // Check if tasks.json already exists
                        var tasksFile = docTree.findFile("tasks.json")
                        
                        if (tasksFile == null) {
                            // Create new tasks.json file
                            tasksFile = docTree.createFile("application/json", "tasks.json")
                        }
                        
                        if (tasksFile != null) {
                            // Update the file path with the URI of tasks.json
                            taskViewModel.updateFilePath(tasksFile.uri.toString())
                            Toast.makeText(context, "File sync configured successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to create tasks.json file", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Cannot write to selected folder", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Setup failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity", "Error setting up file sync", e)
                }
            }
        }
    )
    
    // Initialize calendar events when the app starts
    // Check and update deadline task horizons on app startup (only if it's a new day)
    LaunchedEffect(Unit) {
        taskViewModel.checkAndUpdateDeadlineTaskHorizons()
        taskViewModel.checkForExternalFileChanges()
    }
    
    // Combined lifecycle observer: check deadline task horizons and refresh calendar events when app comes to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Check and update deadline task horizons when app comes to foreground (in case app was in background at midnight)
                taskViewModel.checkAndUpdateDeadlineTaskHorizons()
                // Check for external file changes when app comes to foreground
                taskViewModel.checkForExternalFileChanges()
                // Refresh calendar events when app comes to foreground
                calendarViewModel.loadEvents()
                // Refresh app list (picks up newly pinned shortcuts)
                launcherViewModel.refreshApps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    LaunchedEffect(Unit) {
        // Wait for settings to be loaded first - collect only the first item
        settingsViewModel.settings.take(1).collect { _ ->
            settingsLoaded = true
            // Check calendar permission and request if needed
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CALENDAR
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            } else {
                calendarViewModel.initialize()
            }
        }
    }
    
    // Calendar selection related state
    val availableCalendars by calendarViewModel.availableCalendars.collectAsState()
    
    // Settings related state
    val appSettings by settingsViewModel.settings.collectAsState()
    val showSettingsDialog by settingsViewModel.showSettingsDialog.collectAsState()
    val showCalendarSelection by settingsViewModel.showCalendarSelection.collectAsState()
    val showColorPaletteSelection by settingsViewModel.showColorPaletteSelection.collectAsState()

    val showStatisticsScreen by settingsViewModel.showStatisticsScreen.collectAsState()
    val showCalendarSettings by settingsViewModel.showCalendarSettings.collectAsState()
    val showAppearanceSettings by settingsViewModel.showAppearanceSettings.collectAsState()
    val showFreeTimeSettings by settingsViewModel.showFreeTimeSettings.collectAsState()
    val showFileSyncSettings by settingsViewModel.showFileSyncSettings.collectAsState()
    val showPermissionsSettings by settingsViewModel.showPermissionsSettings.collectAsState()
    val showBackupSettings by settingsViewModel.showBackupSettings.collectAsState()
    val showCalculationCalendarSelection by settingsViewModel.showCalculationCalendarSelection.collectAsState()
    
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
    
    // Update calendar view model with DISPLAYED time horizon for free time calculation
    // This ensures the calculation updates when peeking into the next horizon
    LaunchedEffect(displayedTimeHorizon) {
        calendarViewModel.updateTimeHorizon(displayedTimeHorizon)
    }

    val isWelcomeVisible by tutorialViewModel.isWelcomeVisible.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Dark scrim overlay - bottom layer (visual dimming, doesn't block touches)
        if (!zenModeState.isActive) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {}
        }
        
        // Main app content (bottom layer)
        if (showAddScreen) {
            // System back should return to the home screen, like every other
            // full-screen view here.
            BackHandler {
                showAddScreen = false
            }

            // Show the AddTaskScreen on top of the overlay
            AddTaskScreen(
                onAddTask = { title, timeHorizon ->
                    taskViewModel.addTask(title, timeHorizon)
                    showAddScreen = false
                },
                onAddDeadlineTask = { title, deadline, daysNeeded ->
                    taskViewModel.addDeadlineTask(title, deadline, daysNeeded)
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
                        launcherViewModel.launchApp(app.packageName, app.launchIntent, app.shortcutId)
                        showAppDrawer = false
                    },
                    onNotificationPullDown = {
                        coroutineScope.launch {
                            showAppDrawer = false
                        }
                    },
                    onAppSettings = { app ->
                        launcherViewModel.openAppSettings(app.packageName)
                    },
                    onAppUninstall = { app ->
                        launcherViewModel.uninstallApp(app.packageName)
                    },
                    onResetAppCounter = { app ->
                        launcherViewModel.resetAppCounter(app.packageName)
                    }
                )
            }
        } else if (showSettingsDialog) {
            // Handle back press for settings dialog
            BackHandler {
                settingsViewModel.hideSettingsDialog()
            }
            
            // Show settings as a full screen replacement for main content
            SettingsDialog(
                appSettings = appSettings,
                onDismiss = { settingsViewModel.hideSettingsDialog() },
                onCalendarSettingsClick = { settingsViewModel.showCalendarSettings() },
                onAppearanceSettingsClick = { settingsViewModel.showAppearanceSettings() },
                onFreeTimeSettingsClick = { settingsViewModel.showFreeTimeSettings() },
                onFileSyncSettingsClick = { settingsViewModel.showFileSyncSettings() },
                onPermissionsSettingsClick = { settingsViewModel.showPermissionsSettings() },
                onBackupSettingsClick = { settingsViewModel.showBackupSettings() },
                onStatisticsClick = { settingsViewModel.showStatisticsScreen() },
                onStartTutorial = {
                    settingsViewModel.hideSettingsDialog()
                    tutorialViewModel.startTutorial()
                }
            )
        } else if (showCalendarSelection) {
            // Handle back press for calendar selection
            BackHandler {
                settingsViewModel.hideCalendarSelection()
            }
            
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
                    calendarViewModel.selectAllCalendars()
                },
                onSelectNone = {
                    calendarViewModel.deselectAllCalendars()
                }
            )
        } else if (showColorPaletteSelection) {
            // Handle back press for color palette selection
            BackHandler {
                settingsViewModel.hideColorPaletteSelection()
            }
            
            // Show color palette selection
            ColorPaletteSelectionDialog(
                currentColorTheme = appSettings.colorTheme,
                onColorSelect = { colorTheme ->
                    settingsViewModel.updateColorTheme(colorTheme)
                    settingsViewModel.hideColorPaletteSelection()
                },
                onDismiss = {
                    settingsViewModel.hideColorPaletteSelection()
                }
            )
        } else if (showStatisticsScreen) {
            // Handle back press for statistics screen
            BackHandler {
                settingsViewModel.hideStatisticsScreen()
            }
            
            // Show statistics screen
            StatisticsScreen(
                onBackClick = { settingsViewModel.hideStatisticsScreen() }
            )
        } else if (showCalendarSettings) {
            // Handle back press for calendar settings
            BackHandler {
                settingsViewModel.hideCalendarSettings()
            }
            
            // Show calendar settings screen
            CalendarSettingsScreen(
                appSettings = appSettings,
                onBack = { settingsViewModel.hideCalendarSettings() },
                onToggleCalendarWidget = { settingsViewModel.toggleCalendarWidget() },
                onCalendarDaysChange = { days -> 
                    settingsViewModel.updateCalendarDaysToShow(days)
                    // Reload calendar events with new days setting
                    calendarViewModel.loadEvents(days)
                },
                onCalendarSelectionClick = { settingsViewModel.showCalendarSelection() }
            )
        } else if (showAppearanceSettings) {
            // Handle back press for appearance settings
            BackHandler {
                settingsViewModel.hideAppearanceSettings()
            }
            
            // Show appearance settings screen
            AppearanceSettingsScreen(
                appSettings = appSettings,
                onBack = { settingsViewModel.hideAppearanceSettings() },
                onToggleDarkTheme = { settingsViewModel.toggleDarkTheme() },
                onColorPaletteClick = { settingsViewModel.showColorPaletteSelection() }
            )
        } else if (showFreeTimeSettings) {
            // Handle back press for free time settings
            BackHandler {
                settingsViewModel.hideFreeTimeSettings()
            }
            
            // Show free time settings screen
            FreeTimeSettingsScreen(
                appSettings = appSettings,
                onBack = { settingsViewModel.hideFreeTimeSettings() },
                onProductivityRatioChange = { ratio ->
                    settingsViewModel.updateProductivityRatio(ratio)
                },
                onCalculationCalendarSelectionClick = { 
                    settingsViewModel.showCalculationCalendarSelection()
                }
            )
        } else if (showFileSyncSettings) {
            // Handle back press for file sync settings
            BackHandler {
                settingsViewModel.hideFileSyncSettings()
            }
            
            // Show file sync settings screen
            FileSyncSettingsScreen(
                appSettings = appSettings,
                onBack = { settingsViewModel.hideFileSyncSettings() },
                onUpdateFilePath = { path ->
                    taskViewModel.updateFilePath(path)
                },
                onSetupFileSync = {
                    folderPickerLauncher.launch(null)
                },
                onSyncFromFile = {
                    taskViewModel.syncFromFile()
                },
                onSyncToFile = {
                    taskViewModel.syncToFile()
                }
            )
        } else if (showPermissionsSettings) {
            // Handle back press for permissions settings
            BackHandler {
                settingsViewModel.hidePermissionsSettings()
            }

            // Show permissions settings screen
            PermissionsSettingsScreen(
                onBack = { settingsViewModel.hidePermissionsSettings() }
            )
        } else if (showBackupSettings) {
            // Handle back press for backup settings
            BackHandler {
                settingsViewModel.hideBackupSettings()
            }

            // Show backup settings screen
            BackupSettingsScreen(
                appSettings = appSettings,
                onBack = { settingsViewModel.hideBackupSettings() },
                onExportClick = {
                    exportCsvLauncher.launch("tasks_backup.csv")
                },
                onImportClick = {
                    // OpenDocument accepts an array of MIME types
                    importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "*/*"))
                }
            )
        } else if (showCalculationCalendarSelection) {
            // Handle back press for calculation calendar selection
            BackHandler {
                settingsViewModel.hideCalculationCalendarSelection()
            }
            
            // Map calendars with calculation selection state
            val calendarsWithCalculationSelection = availableCalendars.map { calendar ->
                calendar.copy(selected = calendar.id in appSettings.calculationCalendarIds)
            }
            
            // Show calculation calendar selection (reuse CalendarSelectionDialog)
            CalendarSelectionDialog(
                availableCalendars = calendarsWithCalculationSelection,
                onCalendarToggle = { calendarId ->
                    // Toggle in calculation calendar IDs
                    val currentIds = appSettings.calculationCalendarIds.toMutableSet()
                    if (calendarId in currentIds) {
                        currentIds.remove(calendarId)
                    } else {
                        currentIds.add(calendarId)
                    }
                    settingsViewModel.updateCalculationCalendarIds(currentIds)
                },
                onDismiss = {
                    settingsViewModel.hideCalculationCalendarSelection()
                },
                onSelectAll = {
                    val allIds = availableCalendars.map { it.id }.toSet()
                    settingsViewModel.updateCalculationCalendarIds(allIds)
                },
                onSelectNone = {
                    settingsViewModel.updateCalculationCalendarIds(emptySet())
                }
            )
        } else if (zenModeState.isActive) {
            // Handle back press for zen mode - prevent exiting
            BackHandler {
                // Do nothing - prevent user from exiting zen mode
            }
            
            // Show Zen Mode screen as full-screen surface (like settings dialog)
            ZenModeScreen(
                currentTask = zenModeState.currentTask,
                isActive = zenModeState.isActive,
                onCompleteTask = { _ ->
                    zenModeViewModel.completeCurrentTask()
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (showZenDurationDialog) {
            // Show Zen Mode duration dialog
            ZenModeDurationDialog(
                onDurationSelected = { durationMinutes ->
                    zenModeViewModel.startZenMode(durationMinutes)
                },
                onDismiss = {
                    zenModeViewModel.hideDurationDialog()
                },
                selectedTask = selectedTask
            )
        } else if (isAlignmentActive) {
            // Show alignment screen when active
            AlignmentScreen(
                currentHorizon = currentAlignmentHorizon,
                tasks = tasks,
                onNext = { 
                    alignmentViewModel.moveToNextHorizon() 
                },
                onClose = { 
                    alignmentViewModel.cancelAlignment() 
                },
                onAddTask = { title, timeHorizon ->
                    taskViewModel.addTask(title, timeHorizon)
                },
                onAddDeadlineTask = { title, deadline, daysNeeded ->
                    taskViewModel.addDeadlineTask(title, deadline, daysNeeded)
                },
                onDeleteTask = { taskId ->
                    taskViewModel.deleteTask(taskId)
                },
                onMoveTask = { taskId, newTimeHorizon ->
                    taskViewModel.moveTask(taskId, newTimeHorizon)
                },
                onUpdateTask = { taskId, newTitle, timeHorizon ->
                    taskViewModel.updateTask(taskId, newTitle, timeHorizon)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Home screen content
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    Column {
                        TopAppBar(
                            title = {
                                val title = if (displayedTimeHorizon == TimeHorizon.Today) {
                                    "Today | ${LocalDate.now().format(DateTimeFormatter.ofPattern("d. MMM."))}"
                                } else {
                                    displayedTimeHorizon.title
                                }
                                    Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    if (ContextCompat.checkSelfPermission(
                                                            context,
                                                            Manifest.permission.READ_CALENDAR
                                                        ) == PackageManager.PERMISSION_GRANTED
                                                    ) {
                                                        launcherViewModel.launchCalendarApp("org.fossify.calendar")
                                                    } else {
                                                        calendarViewModel.initialize()
                                                        Toast.makeText(
                                                            context,
                                                            "Calendar permission needed to open calendar app",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                onLongPress = {
                                                    alignmentViewModel.startAlignment()
                                                }
                                            )
                                        },
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                                    )
                                )
                            },
                            modifier = Modifier
                                .padding(top = 12.dp),
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                                scrolledContainerColor = Color.Transparent
                            )
                        )
                        
                        // Progress bar OUTSIDE of the TopAppBar for better visibility
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 2.dp)
                        ) {
                            // Use this simpler direct implementation that should work on all devices
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp) // Make it slimmer
                                    .background(Color.White.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(calculateProgress(displayedTimeHorizon))
                                        .background(MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small)
                                )
                            }
                        }
                        
                        // Free time metric display (Option 2 - below progress bar)
                        val freeTimeMetric by calendarViewModel.freeTimeMetric.collectAsState()
                        freeTimeMetric?.let { metric ->
                            Text(
                                text = "${metric.productiveHoursAvailable.toInt()}h available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp, vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                floatingActionButton = {
                    MultiFunctionFAB(
                        expanded = isFabExpanded,
                        onExpandedChange = { isFabExpanded = it },
                        isPeeking = isPeeking,
                        onPeekingChange = { isPeeking = it },
                        onAddTask = {
                            isFabExpanded = false
                            showAddScreen = true
                        },
                        onAddCalendarEvent = {
                            isFabExpanded = false
                            // Open the "Add Event" dialog directly in the calendar app
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CALENDAR
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                // Create intent to open the "Add Event" dialog
                                val intent = Intent(Intent.ACTION_INSERT)
                                    .setData(CalendarContract.Events.CONTENT_URI)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // If it fails, fall back to just opening the calendar app
                                    Log.e("MainActivity", "Failed to open add event dialog", e)
                                    launcherViewModel.launchCalendarApp("org.fossify.calendar")
                                    Toast.makeText(
                                        context,
                                        "Couldn't open add event directly",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                calendarViewModel.initialize()
                                Toast.makeText(
                                    context,
                                    "Calendar permission needed to add calendar events",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
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
                                    if (appSettings.enableDoubleTapForDialer) {
                                        launcherViewModel.launchDialerApp()
                                    }
                                },
                                onLongPress = {
                                    if (!isAlignmentActive) {
                                        settingsViewModel.showSettingsDialog()
                                    }
                                },
                                onTap = {
                                    if (isFabExpanded) {
                                        isFabExpanded = false
                                    }
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
                        val availableScreenHeight = LocalConfiguration.current.screenHeightDp.dp
                        
                        // Calculate the task list height based on number of tasks
                        // Give each task ~76dp (card + padding) and limit to 1/2 of screen height
                        val taskCount = currentTasks.size
                        val estimatedTaskHeight = 76.dp * taskCount + 48.dp // Add padding
                        val maxTaskListHeight = availableScreenHeight * 0.45f // Reduced to 45% to ensure space for whitespace
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
                                onCompleteTask = taskViewModel::completeTask,
                                onMoveTask = taskViewModel::moveTask,
                                onLongPressTask = { task ->
                                    zenModeViewModel.startZenModeDirectly(task)
                                }
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
                                // Only display calendar data when settings have been loaded
                                if (!settingsLoaded) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                } else if (hasCalendarPermission) {
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
                                                // Open the specific calendar event
                                                launcherViewModel.openCalendarEvent(event.eventId, event.calendarId)
                                            },
                                            modifier = Modifier.fillMaxSize()
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
                                            // For mock events, just request calendar permission
                                            calendarViewModel.initialize()
                                        },
                                        modifier = Modifier.fillMaxSize()
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
                    
                    // Add additional FAB area gesture detection to ensure swipes in this area
                    // trigger the app drawer rather than scrolling calendar events
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .width(80.dp)
                            .height(350.dp) // Increased from 200dp to 350dp to extend higher up the screen
                            .padding(bottom = 100.dp) // Position above the bottom app drawer area
                            .pointerInput(Unit) {
                                var totalDragDistance = 0f

                                detectVerticalDragGestures(
                                    onDragStart = {
                                        totalDragDistance = 0f
                                    },
                                    onDragEnd = {
                                        if (totalDragDistance < -20f) {
                                            showAppDrawer = true
                                        }
                                    },
                                    onDragCancel = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume() // Always consume to prevent calendar scrolling
                                        totalDragDistance += dragAmount

                                        if (totalDragDistance < -30f) {
                                            showAppDrawer = true
                                        }
                                    }
                                )
                            }
                    )
                    
                    // Add a top swipe area at the very top edge for better accessibility
                    // The detection area is just at the edge and no longer in the middle of the screen
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(24.dp) // Much thinner strip at the very top
                            // No more offset! This places it at the top edge
                            .pointerInput(Unit) {
                                var totalDragDistance = 0f
                                
                                detectVerticalDragGestures(
                                    onDragStart = { 
                                        totalDragDistance = 0f
                                    },
                                    onDragEnd = { 
                                        if (totalDragDistance < -20f) {
                                            showAppDrawer = true
                                        }
                                    },
                                    onDragCancel = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume() // Always consume to prevent calendar scrolling
                                        totalDragDistance += dragAmount
                                        
                                        if (totalDragDistance < -30f) {
                                            showAppDrawer = true
                                        }
                                    }
                                )
                            }
                    )
                }
            }
        }

        // Welcome narration — shown on first launch and replayable from Settings.
        // Once the narration is done, a storage location is mandatory: tasks are
        // persisted to a user-chosen JSON file, and without one created tasks
        // cannot be saved or shown. The gate also catches the case where a path
        // is ever lost, instead of silently dropping tasks.
        if (isWelcomeVisible) {
            WelcomeScreen(
                primaryColor = MaterialTheme.colorScheme.primary,
                onComplete = { tutorialViewModel.dismissTutorial() }
            )
        } else if (settingsLoaded && appSettings.tutorialCompleted && appSettings.taskFilePath.isEmpty()) {
            StorageSetupScreen(
                primaryColor = MaterialTheme.colorScheme.primary,
                onChooseFolder = { folderPickerLauncher.launch(null) }
            )
        }
    }
}

/**
 * Multi-functional Floating Action Button with expandable options
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultiFunctionFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isPeeking: Boolean,
    onPeekingChange: (Boolean) -> Unit,
    onAddTask: () -> Unit,
    onAddCalendarEvent: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val taskButtonColor = MaterialTheme.colorScheme.secondary  // Uses primaryVariant1 from theme
    val calendarButtonColor = MaterialTheme.colorScheme.secondary  // Uses primaryVariant2 from theme
    
    // Animation specs for smoother animations
    val enterTransition = remember {
        fadeIn(animationSpec = tween(150)) + 
        expandVertically(
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            expandFrom = Alignment.CenterVertically
        )
    }
    
    val exitTransition = remember {
        fadeOut(animationSpec = tween(100)) + 
        shrinkVertically(
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.CenterVertically
        )
    }
    
    // Use Box to align items vertically centered with main FAB
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        // ----------------
        // Expanded options
        // ----------------
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = spacedBy(16.dp),
            modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
        ) {
            // Calendar option FAB - only shown when expanded
            AnimatedVisibility(
                visible = expanded,
                enter = enterTransition,
                exit = exitTransition
            ) {
                FloatingActionButton(
                    onClick = onAddCalendarEvent,
                    containerColor = calendarButtonColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Add Calendar Event",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Task option FAB - only shown when expanded
            AnimatedVisibility(
                visible = expanded,
                enter = enterTransition,
                exit = exitTransition
            ) {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = taskButtonColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Add Task",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(64.dp)) // Space for main FAB
        }
        
        // ----------------
        // Peek button and main FAB
        // ----------------
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Only show the peek button when FAB is not expanded
            AnimatedVisibility(
                visible = !expanded,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                IconButton(
                    onClick = { }, 
                    modifier = Modifier
                        .pointerInteropFilter { event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    onPeekingChange(true)
                                    true
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    onPeekingChange(false)
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
            }
            
            // Main FAB - toggles expansion
            FloatingActionButton(
                onClick = { onExpandedChange(!expanded) },
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                // Animate the plus icon to rotate when expanded
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 45f else 0f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "FabRotation"
                )
                
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (expanded) "Close Menu" else "Open Menu",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
        }
    }
}

/**
 * Calculate progress for a given time horizon
 */
@Composable
private fun calculateProgress(timeHorizon: TimeHorizon): Float {
    val now = LocalDateTime.now()
    
    // Calculate progress and ensure it's at least 1% visible
    return when (timeHorizon) {
        TimeHorizon.Today -> {
            val startOfDay = now.with(LocalTime.MIN)
            val endOfDay = now.with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfDay, endOfDay)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfDay, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisWeek -> {
            val startOfWeek = now.with(LocalTime.MIN).minusDays(now.dayOfWeek.value.toLong() - 1)
            val endOfWeek = startOfWeek.plusDays(6).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfWeek, endOfWeek)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfWeek, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisMonth -> {
            val startOfMonth = now.withDayOfMonth(1).with(LocalTime.MIN)
            val endOfMonth = startOfMonth.plusMonths(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfMonth, endOfMonth)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfMonth, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisYear -> {
            val startOfYear = now.withMonth(1).withDayOfMonth(1).with(LocalTime.MIN)
            val endOfYear = startOfYear.plusYears(1).minusDays(1).with(LocalTime.MAX)
            val totalMinutes = ChronoUnit.MINUTES.between(startOfYear, endOfYear)
            val elapsedMinutes = ChronoUnit.MINUTES.between(startOfYear, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
        TimeHorizon.ThisLife -> {
            // For life, we'll use a fixed age of 100 years as the total duration
            val birthDate = now.minusYears(30) // Assuming 30 years old as a default
            val deathDate = birthDate.plusYears(100)
            val totalMinutes = ChronoUnit.MINUTES.between(birthDate, deathDate)
            val elapsedMinutes = ChronoUnit.MINUTES.between(birthDate, now)
            (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0.01f, 1f)
        }
    }
}
