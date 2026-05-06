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
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                currentTimeHorizon = timeHorizon
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
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (isLongPressed || isZenModeStarting) 2.dp else 1.dp,
            color = if (isLongPressed || isZenModeStarting) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.outline
        ),
        shape = MaterialTheme.shapes.small,
        shadowElevation = elevation.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
            // Left arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Move to smaller horizon",
                modifier = Modifier.alpha(if (offsetX < -20) abs(offsetX/200) else 0f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Task text
            Text(
                text = if (task.isDeadlineTask && task.deadline != null) {
                    "${task.deadline.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))} | ${task.title}"
                } else {
                    task.title
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
            
            // Right arrow indicator
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Move to larger horizon",
                modifier = Modifier.alpha(if (offsetX > 20) offsetX/200 else 0f),
                tint = MaterialTheme.colorScheme.primary
            )
            
            // Completion icon
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Complete Task",
                tint = if (isCompleted) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
} 
