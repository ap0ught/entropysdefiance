// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

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
import androidx.compose.ui.platform.LocalView
import com.hwanderer.entropysdefiance.model.AppInfo
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
    leftHanded: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val gestureHandler = remember { GestureHandler() }

    // The list previously lived inside a ModalBottomSheet, which provided drag-to-dismiss.
    // Now that the drawer is a plain composable, this nested-scroll connection takes over both
    // jobs: it swallows the fling bounce at the top, and turns a deliberate downward drag once
    // the list is pinned at the top into a close gesture (pull down to dismiss).
    val density = LocalDensity.current
    val pullToCloseThresholdPx = with(density) { 72.dp.toPx() }
    val currentOnPullDown by rememberUpdatedState(onNotificationPullDown)
    val topOverscrollHandler = remember(pullToCloseThresholdPx) {
        object : NestedScrollConnection {
            private var sawUserDrag = false
            private var pulled = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.Drag) {
                    sawUserDrag = true
                    // Scrolling back up cancels any pending pull-to-close.
                    if (available.y < 0f) pulled = 0f
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.Drag && available.y > 0f) {
                    // The list is pinned at the top and can't take the downward drag — treat the
                    // accumulated pull as a dismiss, and consume it so there's no overscroll glow.
                    pulled += available.y
                    if (pulled >= pullToCloseThresholdPx) {
                        pulled = 0f
                        currentOnPullDown()
                    }
                    return available
                }
                return if (source == NestedScrollSource.Fling) available else Offset.Zero
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                val swallow = !sawUserDrag
                sawUserDrag = false
                pulled = 0f
                return if (swallow) available else Velocity.Zero
            }
        }
    }

    // Track which app is showing the action menu (if any)
    var appShowingMenu by remember { mutableStateOf<String?>(null) }

    // Confirmation dialog states
    var showUninstallDialog by remember { mutableStateOf<AppInfo?>(null) }
    var showResetCounterDialog by remember { mutableStateOf<AppInfo?>(null) }

    // Keyboard visibility follows the scroll position: visible near the top (to search), hidden
    // once you scroll down to browse. This is declarative and driven by *focus* rather than
    // SoftwareKeyboardController — gaining/losing focus is the reliable way to show/hide the IME,
    // and it sidesteps the show↔hide insets feedback loop the earlier event-based versions fought.
    //
    // Because it reacts continuously to the scroll offset (not to drag-release), a gentle scroll
    // back toward the top brings the keyboard up exactly like a fast fling does — no need to land
    // pixel-perfect at offset 0. Hysteresis (show under ~160dp from the top, hide only past ~420dp)
    // keeps it from flickering around a single threshold.
    val focusManager = LocalFocusManager.current
    val showKeyboardOffsetPx = with(density) { 160.dp.roundToPx() }
    val hideKeyboardOffsetPx = with(density) { 420.dp.roundToPx() }
    var keyboardWanted by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow {
            // Distance of the list from the very top, in px (large once past the first item).
            if (listState.firstVisibleItemIndex > 0) Int.MAX_VALUE
            else listState.firstVisibleItemScrollOffset
        }.collect { distanceFromTop ->
            if (distanceFromTop <= showKeyboardOffsetPx) {
                keyboardWanted = true
            } else if (distanceFromTop >= hideKeyboardOffsetPx) {
                keyboardWanted = false
            }
        }
    }
    LaunchedEffect(keyboardWanted) {
        if (keyboardWanted) {
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
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
                    onValueChange = { newQuery -> searchQuery = newQuery },
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
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(topOverscrollHandler)
                ) {
                    items(filteredApps, key = { it.listKey }) { app ->
                        AppItem(
                            app = app,
                            onClick = {
                                onAppClick(app)
                                appShowingMenu = null // Close menu when app is clicked
                            },
                            onLongPress = {
                                // Toggle the action menu for this app. Keyed by the unique listKey
                                // so packages with multiple launcher activities don't open menus
                                // on every sibling entry at once.
                                appShowingMenu = if (appShowingMenu == app.listKey) {
                                    null
                                } else {
                                    app.listKey
                                }
                            },
                            isShowingMenu = appShowingMenu == app.listKey,
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
                            },
                            leftHanded = leftHanded
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
    isShowingMenu: Boolean = false,
    onSettings: () -> Unit = {},
    onUninstall: () -> Unit = {},
    onResetCounter: () -> Unit = {},
    modifier: Modifier = Modifier,
    leftHanded: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // App name row
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (leftHanded) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = if (leftHanded) TextAlign.Start else TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth() // Fill width to be part of scrollable area
                    .padding(start = 16.dp, end = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { /* Empty implementation to allow scrolling */ },
                            onLongPress = { onLongPress() },
                            onTap = { onClick() }
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
                horizontalArrangement = if (leftHanded) Arrangement.Start else Arrangement.End,
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
