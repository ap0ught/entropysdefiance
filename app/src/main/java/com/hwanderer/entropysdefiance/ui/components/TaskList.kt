// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.foundation.ExperimentalFoundationApi
import com.hwanderer.entropysdefiance.ui.theme.Motion
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.CheckCircle

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    timeHorizon: TimeHorizon,
    tasks: List<Task>,
    onCompleteTask: (String) -> Unit,
    onMoveTask: (String, TimeHorizon) -> Unit,
    onLongPressTask: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val horizonTasks = remember(tasks, timeHorizon) {
        tasks.filter { it.timeHorizon == timeHorizon }
    }

    LazyColumn(
        modifier = modifier,
        // 24dp start matches the calendar agenda below, so the two lists share a left edge.
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(
            items = horizonTasks,
            key = { _, task -> task.id }
        ) { _, task ->
            TaskItem(
                task = task,
                onComplete = { onCompleteTask(task.id) },
                onMoveTask = { newTimeHorizon -> onMoveTask(task.id, newTimeHorizon) },
                onLongPress = { onLongPressTask(task) },
                onSwipe = {},
                currentTimeHorizon = timeHorizon,
                // Rows close the gap instead of jumping. This matters more here than in a
                // typical list: the task file is polled every two seconds, so edits made on
                // the laptop TUI arrive on their own while you are looking at the screen.
                modifier = Modifier.animateItemPlacement(Motion.standard())
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onComplete: () -> Unit,
    onMoveTask: (TimeHorizon) -> Unit,
    onLongPress: () -> Unit,
    onSwipe: () -> Unit,
    currentTimeHorizon: TimeHorizon,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isCompleted by remember { mutableStateOf(false) }
    var isLongPressed by remember { mutableStateOf(false) }
    var isZenModeStarting by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val swipeThreshold = 100f
    val alpha by animateFloatAsState(
        targetValue = when {
            isZenModeStarting -> 0.3f
            abs(offsetX) > swipeThreshold -> 0.5f
            else -> 1f
        },
        label = "alpha"
    )
    
    val rotation by animateFloatAsState(
        targetValue = offsetX * 0.03f,
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = when {
            isZenModeStarting -> 1.1f
            isLongPressed -> 0.95f
            else -> 1f
        },
        label = "scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isLongPressed || isZenModeStarting) 8f else 0f,
        label = "elevation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .alpha(alpha)
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    if (!isZenModeStarting) {
                        offsetX = (offsetX + delta).coerceIn(-200f, 200f)
                    }
                },
                onDragStopped = {
                    if (!isZenModeStarting && abs(offsetX) > swipeThreshold) {
                        // Determine the new time horizon based on swipe direction
                        val newTimeHorizon = if (offsetX > 0) {
                            // Right swipe - move to larger horizon
                            val horizons = TimeHorizon.entries
                            val nextIndex = (currentTimeHorizon.ordinal + 1).coerceAtMost(horizons.size - 1)
                            horizons[nextIndex]
                        } else {
                            // Left swipe - move to smaller horizon
                            val horizons = TimeHorizon.entries
                            val prevIndex = (currentTimeHorizon.ordinal - 1).coerceAtLeast(0)
                            horizons[prevIndex]
                        }
                        
                        if (newTimeHorizon != currentTimeHorizon) {
                            onMoveTask(newTimeHorizon)
                            onSwipe()
                        }
                    }
                    // Animate back to center
                    offsetX = 0f
                }
            ),
        // No border, no shape, no elevation. The app drawer and the alignment screen both
        // render a list of things as pure type; the home screen was the only surface wrapping
        // them in cards, and a drop shadow over a translucent wallpaper reads as muddy anyway.
        // Being picked up for Zen Mode is now said with the accent on the text itself.
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                // The 6dp matches the inner padding on an agenda row, so that both lists put
                // their rule at 24 + 6 = 30dp from the screen edge. Without it the two rules
                // sat 6dp apart, which reads as a wobble down the left edge of the screen.
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            if (!isCompleted && !isZenModeStarting) {
                                isCompleted = true
                                coroutineScope.launch {
                                    delay(500)
                                    onComplete()
                                }
                            }
                        },
                        onLongPress = {
                            if (!isCompleted && !isZenModeStarting) {
                                isLongPressed = true
                                isZenModeStarting = true
                                coroutineScope.launch {
                                    delay(300) // Brief animation delay
                                    onLongPress()
                                    delay(100) // Brief delay before reset
                                    isLongPressed = false
                                    isZenModeStarting = false
                                }
                            }
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The same 3dp rule the calendar agenda uses one section below. Without an
            // anchor at the left edge a task is a word floating in the middle of the screen;
            // with one, the two lists on this screen read as the same kind of thing. Accent
            // when a deadline is attached — that is the row that carries time pressure.
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(
                        color = if (task.isDeadlineTask) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        } else {
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                        },
                        // Rounded caps, as the agenda's bar has. On a 3dp width the radius
                        // clamps to half the width, so both read as the same drawn stroke.
                        shape = MaterialTheme.shapes.small
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Left arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Move to smaller horizon",
                modifier = Modifier.alpha(if (offsetX < -20) abs(offsetX/200) else 0f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Task text
            // A deadline is the one thing on a task that carries time pressure, so it is the
            // one thing that earns the accent — which also distinguishes deadline tasks now
            // that there is no card to vary.
            Text(
                text = buildAnnotatedString {
                    if (task.isDeadlineTask && task.deadline != null) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append(
                                task.deadline.format(
                                    java.time.format.DateTimeFormatter.ofPattern("dd.MM.")
                                )
                            )
                        }
                        append("  ")
                    }
                    append(task.title)
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = if (isLongPressed || isZenModeStarting) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            
            // Right arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Move to larger horizon",
                modifier = Modifier.alpha(if (offsetX > 20) offsetX/200 else 0f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // An outline until it is used, and dimmer than the task name. Filled, it was the
            // heaviest thing in the row — the eye went to the control rather than to the task,
            // across the empty gap that made the list feel loose.
            Icon(
                imageVector = if (isCompleted) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Outlined.CheckCircle
                },
                contentDescription = "Complete Task",
                modifier = Modifier.size(22.dp),
                tint = if (isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                }
            )
        }
    }
} 
