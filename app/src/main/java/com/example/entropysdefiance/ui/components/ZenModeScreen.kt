package com.example.entropysdefiance.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Zen Mode Screen - A focused, distraction-free interface showing only the next task
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenModeScreen(
    currentTask: Task?,
    isActive: Boolean,
    onCompleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    
    // Animation states
    var isTaskCompleted by remember { mutableStateOf(false) }
    var showCompletionAnimation by remember { mutableStateOf(false) }
    
    // Breathing animation for the task card
    val breathingScale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )
    
    
    // Completion animation
    LaunchedEffect(isTaskCompleted) {
        if (isTaskCompleted) {
            showCompletionAnimation = true
            delay(2000)
            showCompletionAnimation = false
            isTaskCompleted = false
        }
    }
    
    // Full screen surface like settings dialog and app drawer
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Light scrim overlay for better readability
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.3f)
            ) {}
        // Main content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main task display
            if (currentTask != null) {
                ZenTaskCard(
                    task = currentTask,
                    scale = breathingScale,
                    onComplete = {
                        isTaskCompleted = true
                        onCompleteTask(currentTask.id)
                    },
                    isActive = isActive
                )
            } else {
                // No tasks available
                NoTasksMessage()
            }
            
        }
        
            // Completion animation overlay
            if (showCompletionAnimation) {
                CompletionAnimationOverlay()
            }
        }
    }
}


// Motivational messages for zen mode
private val motivationalMessages = listOf(
    "You've got this! 💪",
    "One step at a time ✨",
    "Focus and conquer 🎯",
    "You're doing great! 🌟",
    "Stay focused, stay strong 💫",
    "Every moment counts ⏰",
    "You're unstoppable! 🚀",
    "Breathe and focus 🧘",
    "You're making progress! 📈",
    "Keep pushing forward! 🔥"
)

@Composable
private fun ZenTaskCard(
    task: Task,
    scale: Float,
    onComplete: () -> Unit,
    isActive: Boolean
) {
    val cardScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "cardScale"
    )
    
    // Simple task display without overlay
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .clickable { if (isActive) onComplete() }
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Motivational message
        val motivationalMessage = remember(task.id) {
            motivationalMessages.random()
        }
        Text(
            text = motivationalMessage,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
        
        // Task title
        Text(
            text = task.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            lineHeight = 32.sp
        )
        
        // Completion hint
        if (isActive) {
            Text(
                text = "Tap to complete",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NoTasksMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No tasks available",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Add some tasks to begin Zen mode",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun CompletionAnimationOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "completionScale"
        )
        
        val alpha by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(1000),
            label = "completionAlpha"
        )
        
        Box(
            modifier = Modifier
                .graphicsLayer { 
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(32.dp)
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Task Completed",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/**
 * Zen Mode Entry Dialog with Water Drop Animation
 */
@Composable
fun ZenModeDurationDialog(
    onDurationSelected: (Int) -> Unit, // duration in minutes (not used anymore)
    onDismiss: () -> Unit,
    selectedTask: Task? = null
) {
    var isAnimating by remember { mutableStateOf(false) }
    
    // Water drop animation states
    val dropScale by animateFloatAsState(
        targetValue = if (isAnimating) 20f else 1f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "dropScale"
    )
    
    val dropAlpha by animateFloatAsState(
        targetValue = if (isAnimating) 0f else 1f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "dropAlpha"
    )
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0.8f,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )
    
    // Trigger zen mode after animation
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            delay(1500) // Wait for animation to complete
            onDurationSelected(0) // Duration doesn't matter anymore
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .clickable { if (!isAnimating) onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        if (!isAnimating) {
            // Simple confirmation dialog
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clickable { /* Prevent dismiss when clicking card */ },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Task info if available
                    selectedTask?.let { task ->
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Focus until complete",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    } ?: run {
                        Text(
                            text = "Focus until complete",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    // Action buttons
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Enter Zen Mode button
                        Button(
                            onClick = { isAnimating = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Enter Zen Mode",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        // Cancel button
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        
        // Water drop animation overlay
        if (isAnimating) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .graphicsLayer {
                        scaleX = dropScale
                        scaleY = dropScale
                        alpha = dropAlpha
                    }
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}

