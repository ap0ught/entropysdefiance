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
    onNotificationPullDown: () -> Unit = {}, // Added callback for notification panel
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val gestureHandler = remember { GestureHandler() }
    
    // Track scroll state for keyboard visibility
    var isScrolling by remember { mutableStateOf(false) }
    var lastScrollOffset by remember { mutableIntStateOf(0) }
    
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
                        searchQuery = newQuery
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
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
                    items(filteredApps, key = { it.packageName }) { app ->
                        AppItem(
                            app = app,
                            onClick = { onAppClick(app) },
                            onLongPress = { 
                                // TODO: Implement long press functionality
                                // For now, just open the app
                                onAppClick(app) 
                            },
                            onDoubleTap = { 
                                // TODO: Implement double tap functionality
                                // For now, just open the app
                                onAppClick(app)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    val doubleTapTimeoutMs = 300L
    
    Box(
        modifier = modifier.fillMaxWidth(),
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
} 