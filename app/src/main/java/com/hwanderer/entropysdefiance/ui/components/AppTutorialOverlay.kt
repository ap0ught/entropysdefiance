package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

import com.hwanderer.entropysdefiance.model.TooltipPosition
import com.hwanderer.entropysdefiance.model.TutorialStep
import com.hwanderer.entropysdefiance.viewmodel.TutorialViewModel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

@Composable
fun AppTutorialOverlay(
    tutorialViewModel: TutorialViewModel,
    onTutorialExit: () -> Unit
) {
    val tutorialSteps = remember { tutorialViewModel.getAllSteps() }
    val tutorialState by tutorialViewModel.tutorialState.collectAsState()
    val view = LocalView.current
    val configuration = LocalConfiguration.current

    val currentStepIndex = tutorialState.currentStepIndex ?: 0
    val currentStep = tutorialSteps.getOrNull(currentStepIndex)
    val isCompleted = currentStepIndex >= tutorialSteps.size

    if (tutorialState.isActive && !isCompleted && currentStep != null) {
        val targetRegistry = LocalTutorialTargets.current
        val windowOffset = remember { IntArray(2) }
        LaunchedEffect(view, configuration) {
            view.getLocationOnScreen(windowOffset)
        }

        val targetBoundsInScreen = targetRegistry.getBounds(currentStep.targetId)
        val targetBounds = targetBoundsInScreen?.let { bounds ->
            Rect(
                left = bounds.left - windowOffset[0],
                top = bounds.top - windowOffset[1],
                right = bounds.right - windowOffset[0],
                bottom = bounds.bottom - windowOffset[1]
            )
        }

        // Scrim with spotlight hole — blocks touches outside the target, passes through inside
        TutorialScrim(bounds = targetBounds)

        // Animated rings drawn on top of the spotlight hole
        targetBounds?.let { bounds ->
            SpotlightRings(bounds = bounds)
        }

        // Tooltip as a non-focusable popup — sits in its own window, unaffected by scrim
        TooltipPopup(
            step = currentStep,
            currentIndex = currentStepIndex,
            totalSteps = tutorialSteps.size,
            targetBounds = targetBounds,
            onDismiss = { tutorialViewModel.exitTutorial() }
        )
    }

    if (isCompleted && tutorialState.isActive) {
        TutorialCompletionOverlay(
            onComplete = { tutorialViewModel.exitTutorial() }
        )
    }
}

@Composable
private fun TutorialScrim(bounds: Rect?) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.65f))
            if (bounds != null) {
                val center = Offset(bounds.center.x, bounds.center.y)
                val radius = bounds.width.coerceAtLeast(bounds.height) / 2f + 40f
                drawCircle(
                    color = Color.Black,
                    radius = radius,
                    center = center,
                    blendMode = BlendMode.Clear
                )
            }
        }

        if (bounds == null) return@BoxWithConstraints

        val inflatedBounds = bounds.inflate(64f)
        val holeLeft = inflatedBounds.left.coerceIn(0f, screenWidthPx)
        val holeTop = inflatedBounds.top.coerceIn(0f, screenHeightPx)
        val holeRight = inflatedBounds.right.coerceIn(0f, screenWidthPx)
        val holeBottom = inflatedBounds.bottom.coerceIn(0f, screenHeightPx)

        val topHeight = holeTop.coerceAtLeast(0f)
        val bottomHeight = (screenHeightPx - holeBottom).coerceAtLeast(0f)
        val leftWidth = holeLeft.coerceAtLeast(0f)
        val rightWidth = (screenWidthPx - holeRight).coerceAtLeast(0f)
        val middleHeight = (holeBottom - holeTop).coerceAtLeast(0f)

        fun Modifier.blockTouches(): Modifier = pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.forEach { it.consume() }
                }
            }
        }

        if (topHeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { topHeight.toDp() })
                    .blockTouches()
            )
        }

        if (bottomHeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { bottomHeight.toDp() })
                    .offset(y = with(density) { holeBottom.toDp() })
                    .blockTouches()
            )
        }

        if (leftWidth > 0f && middleHeight > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { leftWidth.toDp() })
                    .height(with(density) { middleHeight.toDp() })
                    .offset(y = with(density) { holeTop.toDp() })
                    .blockTouches()
            )
        }

        if (rightWidth > 0f && middleHeight > 0f) {
            Box(
                modifier = Modifier
                    .width(with(density) { rightWidth.toDp() })
                    .height(with(density) { middleHeight.toDp() })
                    .offset(
                        x = with(density) { holeRight.toDp() },
                        y = with(density) { holeTop.toDp() }
                    )
                    .blockTouches()
            )
        }
    }
}

@Composable
private fun SpotlightRings(bounds: Rect) {
    val infiniteTransition = rememberInfiniteTransition(label = "spotlightPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    // fillMaxSize Canvas with no pointer modifiers — all touches pass through
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(bounds.center.x, bounds.center.y)
        val baseRadius = bounds.width.coerceAtLeast(bounds.height) / 2f
        val expandedRadius = baseRadius * pulseScale

        drawCircle(
            color = primaryColor.copy(alpha = pulseAlpha * 0.5f),
            radius = expandedRadius + 20f,
            center = center,
            style = Stroke(width = 4f)
        )

        drawCircle(
            color = primaryColor.copy(alpha = pulseAlpha),
            radius = expandedRadius + 10f,
            center = center,
            style = Stroke(width = 2f)
        )
    }
}

@Composable
private fun TooltipPopup(
    step: TutorialStep,
    currentIndex: Int,
    totalSteps: Int,
    targetBounds: Rect?,
    onDismiss: () -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val cardWidthDp = 280
    val cardWidthPx = with(density) { cardWidthDp.dp.toPx() }
    val marginPx = with(density) { 12.dp.toPx() }
    val gapPx = with(density) { 16.dp.toPx() }

    // Measure the actual card height, then use it for positioning
    var measuredCardHeight by remember { mutableFloatStateOf(0f) }
    var measuredCardWidth by remember { mutableFloatStateOf(0f) }

    fun computeOffset(cardWidthPx: Float, cardHeightPx: Float): IntOffset {
        if (targetBounds == null) {
            return IntOffset(
                x = ((screenWidthPx - cardWidthPx) / 2f).toInt(),
                y = (screenHeightPx * 0.15f).toInt()
            )
        }

        val spaceAbove = (targetBounds.top - marginPx).coerceAtLeast(0f)
        val spaceBelow = (screenHeightPx - targetBounds.bottom - marginPx).coerceAtLeast(0f)
        val spaceLeft = (targetBounds.left - marginPx).coerceAtLeast(0f)
        val spaceRight = (screenWidthPx - targetBounds.right - marginPx).coerceAtLeast(0f)

        fun fitsAbove(): Boolean = cardHeightPx + gapPx <= spaceAbove
        fun fitsBelow(): Boolean = cardHeightPx + gapPx <= spaceBelow
        fun fitsLeft(): Boolean = cardWidthPx + gapPx <= spaceLeft
        fun fitsRight(): Boolean = cardWidthPx + gapPx <= spaceRight

        val preferred = when (step.position) {
            TooltipPosition.TOP -> TooltipPosition.TOP
            TooltipPosition.BOTTOM -> TooltipPosition.BOTTOM
            TooltipPosition.LEFT -> TooltipPosition.LEFT
            TooltipPosition.RIGHT -> TooltipPosition.RIGHT
            TooltipPosition.AUTO -> TooltipPosition.AUTO
        }

        val orderedSides = when (preferred) {
            TooltipPosition.TOP -> listOf(TooltipPosition.TOP, TooltipPosition.BOTTOM, TooltipPosition.RIGHT, TooltipPosition.LEFT)
            TooltipPosition.BOTTOM -> listOf(TooltipPosition.BOTTOM, TooltipPosition.TOP, TooltipPosition.RIGHT, TooltipPosition.LEFT)
            TooltipPosition.LEFT -> listOf(TooltipPosition.LEFT, TooltipPosition.RIGHT, TooltipPosition.TOP, TooltipPosition.BOTTOM)
            TooltipPosition.RIGHT -> listOf(TooltipPosition.RIGHT, TooltipPosition.LEFT, TooltipPosition.TOP, TooltipPosition.BOTTOM)
            TooltipPosition.AUTO -> {
                val bySpace = listOf(
                    TooltipPosition.TOP to spaceAbove,
                    TooltipPosition.BOTTOM to spaceBelow,
                    TooltipPosition.LEFT to spaceLeft,
                    TooltipPosition.RIGHT to spaceRight
                ).sortedByDescending { it.second }.map { it.first }
                bySpace + listOf(TooltipPosition.BOTTOM, TooltipPosition.TOP, TooltipPosition.RIGHT, TooltipPosition.LEFT)
            }
        }

        val fittingSides = orderedSides.filter { side ->
            when (side) {
                TooltipPosition.TOP -> fitsAbove()
                TooltipPosition.BOTTOM -> fitsBelow()
                TooltipPosition.LEFT -> fitsLeft()
                TooltipPosition.RIGHT -> fitsRight()
                TooltipPosition.AUTO -> false
            }
        }

        fun candidateFor(side: TooltipPosition): IntOffset {
            val rawX: Float
            val rawY: Float

            when (side) {
                TooltipPosition.TOP -> {
                    rawX = targetBounds.center.x - cardWidthPx / 2f
                    rawY = targetBounds.top - gapPx - cardHeightPx
                }
                TooltipPosition.BOTTOM -> {
                    rawX = targetBounds.center.x - cardWidthPx / 2f
                    rawY = targetBounds.bottom + gapPx
                }
                TooltipPosition.LEFT -> {
                    rawX = targetBounds.left - gapPx - cardWidthPx
                    rawY = targetBounds.center.y - cardHeightPx / 2f
                }
                TooltipPosition.RIGHT -> {
                    rawX = targetBounds.right + gapPx
                    rawY = targetBounds.center.y - cardHeightPx / 2f
                }
                TooltipPosition.AUTO -> {
                    rawX = targetBounds.center.x - cardWidthPx / 2f
                    rawY = targetBounds.bottom + gapPx
                }
            }

            val clampedX = rawX.coerceIn(marginPx, screenWidthPx - cardWidthPx - marginPx)
            val clampedY = rawY.coerceIn(marginPx, screenHeightPx - cardHeightPx - marginPx)

            return IntOffset(x = clampedX.toInt(), y = clampedY.toInt())
        }

        fun overlapsTarget(candidate: IntOffset): Boolean {
            val tooltipRect = Rect(
                left = candidate.x.toFloat(),
                top = candidate.y.toFloat(),
                right = candidate.x + cardWidthPx,
                bottom = candidate.y + cardHeightPx
            )
            return tooltipRect.overlaps(targetBounds.inflate(gapPx / 2f))
        }

        fun overlapArea(candidate: IntOffset): Float {
            val tooltipRect = Rect(
                left = candidate.x.toFloat(),
                top = candidate.y.toFloat(),
                right = candidate.x + cardWidthPx,
                bottom = candidate.y + cardHeightPx
            )
            val inflatedTarget = targetBounds.inflate(gapPx / 2f)
            val left = maxOf(tooltipRect.left, inflatedTarget.left)
            val top = maxOf(tooltipRect.top, inflatedTarget.top)
            val right = minOf(tooltipRect.right, inflatedTarget.right)
            val bottom = minOf(tooltipRect.bottom, inflatedTarget.bottom)
            return if (right > left && bottom > top) (right - left) * (bottom - top) else 0f
        }

        val sidesToEvaluate = if (fittingSides.isNotEmpty()) fittingSides
            else orderedSides.filter { it != TooltipPosition.AUTO }

        val bestCandidate = sidesToEvaluate
            .map { side -> candidateFor(side) }
            .minByOrNull { candidate -> overlapArea(candidate) }

        val nonOverlapping = bestCandidate?.takeIf { candidate -> !overlapsTarget(candidate) }

        return nonOverlapping ?: bestCandidate ?: candidateFor(TooltipPosition.BOTTOM)
    }

    // Use measured size once available, otherwise a generous initial estimate
    val effectiveHeight = if (measuredCardHeight > 0f) measuredCardHeight
        else with(density) { 300.dp.toPx() }
    val effectiveWidth = if (measuredCardWidth > 0f) measuredCardWidth else cardWidthPx
    val maxCardWidthPx = (screenWidthPx - marginPx * 2).coerceAtLeast(0f)
    val finalCardWidth = effectiveWidth.coerceAtMost(maxCardWidthPx)
    val tooltipOffset = computeOffset(finalCardWidth, effectiveHeight)

    Popup(
        offset = tooltipOffset,
        properties = PopupProperties(
            focusable = false,
            clippingEnabled = false
        )
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = cardWidthDp.dp)
                .onGloballyPositioned { coords ->
                    measuredCardHeight = coords.size.height.toFloat()
                    measuredCardWidth = coords.size.width.toFloat()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Tutorial",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${currentIndex + 1}/$totalSteps",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Text(
                                text = "\u2715",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dotInfiniteTransition = rememberInfiniteTransition(label = "dotPulse")
                    val dotScale by dotInfiniteTransition.animateFloat(
                        initialValue = 0.7f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotScale"
                    )

                    Box(
                        modifier = Modifier
                            .size((10 * dotScale).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Follow the highlight",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Skip tutorial",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialCompletionOverlay(
    onComplete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.75f)
                    )
                )
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onComplete() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "celebrationBounce"
                )

                Text(
                    text = "\uD83C\uDF89",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tutorial Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You're now ready to use Entropy's Defiance. Tap anywhere to begin!",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
