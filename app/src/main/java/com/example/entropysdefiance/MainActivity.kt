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
import com.example.entropysdefiance.viewmodel.TaskViewModel
import com.example.entropysdefiance.viewmodel.LauncherViewModel
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import android.app.Application
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
        
        setContent {
            EntropysDefianceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    TaskApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun TaskApp(
    viewModel: TaskViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    ),
    launcherViewModel: LauncherViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    var showAddScreen by remember { mutableStateOf(false) }
    val tasks by viewModel.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val apps by launcherViewModel.apps.collectAsStateWithLifecycle(initialValue = emptyList())
    var isPeeking by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val coroutineScope = rememberCoroutineScope()
    
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
                    viewModel.addTask(title, timeHorizon)
                    showAddScreen = false
                },
                onDismiss = { showAddScreen = false }
            )
        } else if (showAppDrawer) {
            // Use ModalBottomSheet with proper configuration for full-screen display
            ModalBottomSheet(
                onDismissRequest = { showAppDrawer = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), // Force full expansion
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
                    }
                )
            }
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
                ) {
                    // TaskList (base layer)
                    TaskList(
                        modifier = Modifier.fillMaxSize(),
                        timeHorizon = displayedTimeHorizon,
                        tasks = tasks,
                        onDeleteTask = viewModel::deleteTask,
                        onMoveTask = viewModel::moveTask
                    )
                    
                    // App drawer overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f)
                            .align(Alignment.BottomCenter)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { dragStart = it },
                                    onDragEnd = {},
                                    onDragCancel = {},
                                    onVerticalDrag = { change, dragAmount ->
                                        if (dragAmount < -20f) {
                                            showAppDrawer = true
                                            change.consume()
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