package com.example.entropysdefiance.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.entropysdefiance.model.AppInfo
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.pointerInteropFilter
import android.view.MotionEvent
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

// Gesture handler class to manage different gestures
class GestureHandler {
    companion object {
        // Constants for gesture detection
        private const val DOUBLE_TAP_TIMEOUT = 300L // ms
        private const val LONG_PRESS_DURATION = 500L // ms
        private const val DRAG_THRESHOLD = 50f // dp
        private const val VERTICAL_DRAG_THRESHOLD = 100f // dp
    }
    
    // Create a gesture modifier that handles multiple gestures
    @Composable
    fun createGestureModifier(
        onTap: () -> Unit = {},
        onDoubleTap: () -> Unit = {},
        onLongPress: () -> Unit = {},
        onVerticalSwipeDown: () -> Unit = {},
        enabled: Boolean = true
    ): Modifier {
        val density = LocalDensity.current
        val dragThresholdPx = with(density) { DRAG_THRESHOLD.dp.toPx() }
        val verticalDragThresholdPx = with(density) { VERTICAL_DRAG_THRESHOLD.dp.toPx() }
        
        var lastTapTime by remember { mutableLongStateOf(0L) }
        val coroutineScope = rememberCoroutineScope()
        
        // Track drag state to prevent tap after swipe
        var isDragging by remember { mutableStateOf(false) }
        var totalDragY by remember { mutableFloatStateOf(0f) }
        
        return Modifier
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = { 
                        isDragging = true 
                        totalDragY = 0f
                    },
                    onDragEnd = { 
                        // Reset drag state after a short delay to allow tap detection
                        coroutineScope.launch {
                            delay(100)
                            isDragging = false
                            totalDragY = 0f
                        }
                    },
                    onDragCancel = { 
                        isDragging = false
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        
                        // Accumulate vertical drag amount
                        if (dragAmount.y > 0 && abs(dragAmount.y) > abs(dragAmount.x)) {
                            totalDragY += dragAmount.y
                            // Only trigger swipe down if we've dragged past the threshold
                            if (totalDragY > verticalDragThresholdPx && onVerticalSwipeDown != {}) {
                                onVerticalSwipeDown()
                                // Reset to prevent multiple triggers
                                totalDragY = 0f
                            }
                        }
                    }
                )
            }
            .pointerInput(enabled) {
                coroutineScope {
                    detectTapGestures(
                        onPress = { offset ->
                            // Don't process tap if we're in a drag operation
                            if (isDragging) return@detectTapGestures
                            
                            val pressStartTime = System.currentTimeMillis()
                            val longPressJob = launch {
                                delay(LONG_PRESS_DURATION)
                                if (!isDragging) {
                                    onLongPress()
                                }
                            }
                            
                            tryAwaitRelease()
                            longPressJob.cancel()
                            
                            // Don't process tap if a drag started during press
                            if (isDragging) return@detectTapGestures
                            
                            val pressDuration = System.currentTimeMillis() - pressStartTime
                            if (pressDuration < LONG_PRESS_DURATION) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastTapTime < DOUBLE_TAP_TIMEOUT) {
                                    onDoubleTap()
                                    lastTapTime = 0
                                } else {
                                    lastTapTime = currentTime
                                    launch {
                                        delay(DOUBLE_TAP_TIMEOUT)
                                        if (System.currentTimeMillis() - lastTapTime >= DOUBLE_TAP_TIMEOUT && !isDragging) {
                                            onTap()
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
    }
    
    // Special modifier for global swipe detection that won't interfere with item clicks
    @Composable
    fun createSwipeOnlyModifier(
        onVerticalSwipeDown: () -> Unit,
        enabled: Boolean = true
    ): Modifier {
        val density = LocalDensity.current
        val verticalDragThresholdPx = with(density) { VERTICAL_DRAG_THRESHOLD.dp.toPx() }
        var totalDragY by remember { mutableFloatStateOf(0f) }
        var hasTriggeredSwipe by remember { mutableStateOf(false) }
        
        return Modifier.pointerInput(enabled) {
            detectDragGestures(
                onDragStart = { 
                    totalDragY = 0f
                    hasTriggeredSwipe = false
                },
                onDragEnd = { 
                    totalDragY = 0f
                    hasTriggeredSwipe = false
                },
                onDragCancel = { 
                    totalDragY = 0f
                    hasTriggeredSwipe = false
                },
                onDrag = { change, dragAmount ->
                    // Only consume changes for vertical downward swipes
                    if (dragAmount.y > 0 && abs(dragAmount.y) > abs(dragAmount.x)) {
                        change.consume()
                        totalDragY += dragAmount.y
                        
                        // Check if we've exceeded the threshold and haven't triggered yet
                        if (totalDragY > verticalDragThresholdPx && !hasTriggeredSwipe) {
                            onVerticalSwipeDown()
                            hasTriggeredSwipe = true  // Prevent multiple triggers
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    onNotificationPullDown: () -> Unit = {},
    onAppSettings: (AppInfo) -> Unit = {},
    onAppUninstall: (AppInfo) -> Unit = {},
    onResetAppCounter: (AppInfo) -> Unit = {},
    onSearchFocused: () -> Unit = {},
    onAppLaunched: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var hasStartedTyping by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val gestureHandler = remember { GestureHandler() }
    val tutorialTargetRegistry = LocalTutorialTargets.current
    
    // Track scroll state for keyboard visibility
    var isScrolling by remember { mutableStateOf(false) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    
    // Track which app is showing the action menu (if any)
    var appShowingMenu by remember { mutableStateOf<String?>(null) }
    
    // Confirmation dialog states
    var showUninstallDialog by remember { mutableStateOf<AppInfo?>(null) }
    var showResetCounterDialog by remember { mutableStateOf<AppInfo?>(null) }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    
    // Monitor scroll state to hide/show keyboard
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { scrollOffset ->
                val isAtTop = scrollOffset == 0 && listState.firstVisibleItemIndex == 0
                
                // Hide keyboard when scrolling down, show when at top
                if (scrollOffset > lastScrollOffset && scrollOffset > 10) {
                    // Scrolling down - hide keyboard
                    if (!isScrolling) {
                        isScrolling = true
                        keyboardController?.hide()
                    }
                } else if (isAtTop) {
                    // At top - show keyboard
                    if (isScrolling) {
                        isScrolling = false
                        keyboardController?.show()
                    }
                }
                
                lastScrollOffset = scrollOffset
            }
    }

    // Auto-launch when there's exactly one match in the filtered list
    val filteredApps = if (searchQuery.isEmpty()) {
        apps
    } else {
        apps.filter { app ->
            app.appName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    // Check for auto-launch condition
    LaunchedEffect(filteredApps) {
        if (searchQuery.isNotEmpty() && filteredApps.size == 1) {
            onAppClick(filteredApps[0])
            onAppLaunched()
        }
    }

    // Create a Box to handle global swipe detection separately from app items
    Box(
        modifier = modifier.then(
            gestureHandler.createSwipeOnlyModifier(
                onVerticalSwipeDown = onNotificationPullDown
            )
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(100.dp))
                
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { newQuery ->
                        if (!hasStartedTyping && newQuery.isNotEmpty()) {
                            hasStartedTyping = true
                            onSearchFocused()
                        }
                        searchQuery = newQuery
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned { coordinates ->
                            val bounds = androidx.compose.ui.geometry.Rect(
                                left = coordinates.positionInRoot().x,
                                top = coordinates.positionInRoot().y,
                                right = coordinates.positionInRoot().x + coordinates.size.width,
                                bottom = coordinates.positionInRoot().y + coordinates.size.height
                            )
                            tutorialTargetRegistry.report("app_drawer_search", bounds)
                        },
                    textStyle = MaterialTheme.typography.displayMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(Color.Transparent),
                    decorationBox = { innerTextField ->
                        Column {
                            Box {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search apps...",
                                        style = MaterialTheme.typography.displayMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                thickness = 2.dp
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(70.dp))
                
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.usageKey }) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                onAppClick(app)
                                onAppLaunched()
                                appShowingMenu = null // Close menu when app is clicked
                            },
                            onLongPress = {
                                // Toggle the action menu for this app
                                appShowingMenu = if (appShowingMenu == app.packageName) {
                                    null
                                } else {
                                    app.packageName
                                }
                            },
                            onDoubleTap = {
                                // TODO: Implement double tap functionality
                                // For now, just open the app
                                onAppClick(app)
                                onAppLaunched()
                            },
                            isShowingMenu = appShowingMenu == app.packageName,
                            onSettings = { 
                                onAppSettings(app)
                                appShowingMenu = null // Auto-dismiss
                            },
                            onUninstall = { 
                                showUninstallDialog = app
                                appShowingMenu = null // Auto-dismiss
                            },
                            onResetCounter = { 
                                showResetCounterDialog = app
                                appShowingMenu = null // Auto-dismiss
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Uninstall confirmation dialog
    showUninstallDialog?.let { app ->
        AlertDialog(
            onDismissRequest = { showUninstallDialog = null },
            title = { Text("Uninstall ${app.appName}?") },
            text = { Text("This will open the system uninstall dialog for ${app.appName}.") },
            confirmButton = {
                TextButton(onClick = {
                    onAppUninstall(app)
                    showUninstallDialog = null
                }) {
                    Text("Uninstall")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUninstallDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Reset counter confirmation dialog
    showResetCounterDialog?.let { app ->
        AlertDialog(
            onDismissRequest = { showResetCounterDialog = null },
            title = { Text("Reset counter for ${app.appName}?") },
            text = { Text("This will reset the open counter and change the app's position in the drawer.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAppCounter(app)
                    showResetCounterDialog = null
                }) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetCounterDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    isShowingMenu: Boolean = false,
    onSettings: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onResetCounter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    val doubleTapTimeoutMs = 300L
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // App name row
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth() // Fill width to be part of scrollable area
                    .padding(start = 16.dp, end = 16.dp)
                    // Use combined pointer input for handling gestures while allowing scrolling
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { /* Empty implementation to allow scrolling */ },
                            onDoubleTap = { 
                                onDoubleTap()
                            },
                            onLongPress = { 
                                onLongPress() 
                            },
                            onTap = {
                                val currentTime = System.currentTimeMillis()
                                val isDoubleTap = (currentTime - lastTapTimeMs) < doubleTapTimeoutMs
                                
                                if (!isDoubleTap) {
                                    // Single tap after timeout
                                    onClick()
                                }
                                
                                lastTapTimeMs = currentTime
                            }
                        )
                    }
            )
        }
        
        // Action menu row (shown when long-pressed)
        if (isShowingMenu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                IconButton(
                    onClick = onSettings,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "App Settings",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Uninstall button
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Uninstall App",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Reset counter button
                IconButton(
                    onClick = onResetCounter,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Open Counter",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
} 