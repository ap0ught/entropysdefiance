// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.components

import androidx.compose.animation.core.Animatable
import com.hwanderer.entropysdefiance.ui.theme.Motion
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drives the app drawer as a continuous, finger-tracked transition instead of a boolean flip.
 *
 * [progress] runs 0f (home) → 1f (drawer). Everything on screen derives its transform from it,
 * so a half-finished swipe shows a half-open drawer and can be abandoned. Releasing settles
 * forward or back based on **velocity**, which is what the old distance-only thresholds
 * (`totalDragDistance < -30f`) got wrong: a fast, short flick lifted before ever reaching them.
 *
 * Sign convention follows Compose pointer deltas — dragging *up* is negative y, and opens
 * the drawer.
 */
class DrawerSwipeState(private val scope: CoroutineScope) {

    private val animatable = Animatable(0f).apply {
        // A fast flick hands the spring a large initial velocity, and critical damping only
        // rules out *oscillation* — it still overshoots once. Past 1f the drawer lifts off
        // its resting place and shows a gap; below 0f it unmounts and remounts mid-bounce.
        updateBounds(lowerBound = 0f, upperBound = 1f)
    }

    /** 0f = home fully shown, 1f = drawer fully shown. */
    val progress: Float get() = animatable.value

    // Read during composition, so these are derived: a plain `animatable.value > 0f` getter
    // would recompose the drawer branch on every single animation frame.
    /** True once any part of the drawer is on screen — used to skip composing it when closed. */
    val isVisible: Boolean by derivedStateOf { animatable.value > 0f }

    /** True when the drawer is the settled, interactive surface (back button, clicks). */
    val isOpen: Boolean by derivedStateOf { animatable.targetValue == 1f }

    /**
     * True only once the drawer has actually *arrived*, not merely started heading there.
     * The search field's autofocus keys off this: focusing while the drawer is still being
     * dragged threw the keyboard up mid-swipe.
     */
    val isFullyOpen: Boolean by derivedStateOf { animatable.value >= 1f }

    /** Height the drawer travels, in px. Set from the home container's measured size. */
    var sheetHeightPx: Float = 0f

    // Absolute drag position, accumulated outside the Animatable so a dropped frame
    // self-corrects instead of losing that delta forever.
    private var rawProgress = 0f
    private var dragOrigin = 0f
    private var isDragging = false

    /**
     * Feeds one drag delta in px. Returns the px actually consumed, in the same sign space,
     * so nested scroll can hand back the remainder.
     */
    fun onDrag(deltaPx: Float): Float {
        if (sheetHeightPx <= 0f) return 0f
        if (!isDragging) {
            isDragging = true
            rawProgress = animatable.value
            dragOrigin = animatable.value
        }
        val travelPx = dragTravelPx()
        val before = rawProgress
        rawProgress = (rawProgress + (-deltaPx / travelPx)).coerceIn(0f, 1f)
        val consumedProgress = rawProgress - before
        if (consumedProgress == 0f) return 0f
        scope.launch { animatable.snapTo(rawProgress) }
        return -consumedProgress * travelPx
    }

    /**
     * Finger travel that corresponds to a full open. Less than the drawer's own travel, so the
     * drawer outruns the finger — a short swipe still carries it the whole way, which is what
     * makes it feel snappy rather than laborious.
     */
    private fun dragTravelPx(): Float = sheetHeightPx * DRAG_DISTANCE_FRACTION

    /**
     * Non-suspending entry point for [settle]. Pointer-gesture callbacks run in a
     * `@RestrictsSuspension` scope and cannot await it directly.
     */
    fun onDragStopped(velocityPxPerSec: Float, commitOnAnyUpwardDrag: Boolean = false) {
        scope.launch { settle(velocityPxPerSec, commitOnAnyUpwardDrag) }
    }

    /**
     * Settles open or closed. [velocityPxPerSec] is negative for an upward fling.
     *
     * [commitOnAnyUpwardDrag] distinguishes the two ways the drawer gets dragged. In the
     * dedicated swipe zones an upward drag can mean nothing else, so *any* upward movement
     * commits — being shown the drawer and then having it drop back is the worst outcome, and
     * nobody changes their mind halfway through that gesture. Overscroll off the end of a list
     * is not so unambiguous, so that path keeps a distance threshold and can still fall back.
     */
    suspend fun settle(velocityPxPerSec: Float, commitOnAnyUpwardDrag: Boolean = false) {
        isDragging = false
        if (sheetHeightPx <= 0f) return
        // Commit is measured against where this drag *started*, not against the midpoint, so
        // it is symmetric: a short pull up from closed opens, and an equally short pull down
        // from open closes. A midpoint test would have made closing need twice the travel of
        // opening.
        // `rawProgress`, not `animatable.value`: the drag writes progress through an async
        // snapTo, so the Animatable can lag a frame behind what the finger actually did.
        val moved = rawProgress - dragOrigin
        val target = when {
            // Unconditional, and deliberately ahead of the velocity tests. A brief flick ends
            // with the finger decelerating, and the tracker can report a small *downward*
            // velocity from those last samples — which used to trip the close branch even
            // though every pixel of the gesture went up.
            commitOnAnyUpwardDrag && moved > 0f -> 1f
            velocityPxPerSec < -FLING_VELOCITY_PX_PER_SEC -> 1f
            velocityPxPerSec > FLING_VELOCITY_PX_PER_SEC -> 0f
            moved > 0f -> if (moved >= COMMIT_FRACTION) 1f else 0f
            moved < 0f -> if (-moved >= COMMIT_FRACTION) 0f else 1f
            else -> if (animatable.value >= 0.5f) 1f else 0f
        }
        animatable.animateTo(
            targetValue = target,
            animationSpec = Motion.Settle,
            initialVelocity = -velocityPxPerSec / dragTravelPx()
        )
    }

    fun open() {
        isDragging = false
        scope.launch {
            animatable.animateTo(1f, Motion.Settle)
        }
    }

    fun close() {
        isDragging = false
        scope.launch {
            animatable.animateTo(0f, Motion.Settle)
        }
    }

    private companion object {
        // Anything faster than this settles in the fling direction regardless of how far
        // the finger actually travelled.
        const val FLING_VELOCITY_PX_PER_SEC = 400f

        // Finger travel for a full open, as a fraction of screen height. Below 1.0 the drawer
        // moves further than the finger does.
        const val DRAG_DISTANCE_FRACTION = 0.5f

        // Distance that commits a gesture whose direction is ambiguous — currently only
        // overscroll off the end of a list. The swipe zones ignore this and commit on any
        // upward movement.
        const val COMMIT_FRACTION = 0.3f

    }
}

@Composable
fun rememberDrawerSwipeState(scope: CoroutineScope): DrawerSwipeState =
    remember(scope) { DrawerSwipeState(scope) }

/**
 * Layer 1 — the dedicated swipe zones.
 *
 * Uses touch slop plus a [VelocityTracker] rather than accumulated-distance thresholds, so a
 * quick flick is as reliable as a slow drag. Deltas are only consumed once they actually move
 * the drawer, leaving the gesture available to anything else when the drawer is already at a
 * limit.
 */
suspend fun PointerInputScope.detectDrawerSwipe(state: DrawerSwipeState) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // Stand down once the drawer is showing — from then on its own list owns vertical
        // drags. Checked here rather than by keying the modifier: changing a pointerInput key
        // restarts it, which would cancel the very drag that is opening the drawer.
        if (state.isVisible) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)

        val slopChange = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
            if (state.onDrag(overSlop) != 0f) change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
        } ?: return@awaitEachGesture

        verticalDrag(slopChange.id) { change ->
            if (state.onDrag(change.positionChange().y) != 0f) change.consume()
            tracker.addPosition(change.uptimeMillis, change.position)
        }

        state.onDragStopped(tracker.calculateVelocity().y, commitOnAnyUpwardDrag = true)
    }
}

/**
 * Layer 2 — the nested-scroll gate.
 *
 * The home screen is not inert like a text-only launcher's: the task list and the calendar
 * agenda are both `LazyColumn`s that legitimately want vertical drags. So the drawer never
 * takes a drag outright. Children scroll **first**; only the delta they cannot consume — the
 * list is already at its end — reaches [onPostScroll] and opens the drawer. A downward drag
 * while the drawer is showing closes it first ([onPreScroll]), the usual sheet-over-content
 * ordering.
 */
class DrawerNestedScrollConnection(private val state: DrawerSwipeState) : NestedScrollConnection {

    // Whether the list under the finger actually scrolled during this gesture. If it never
    // did, there was nothing else the drag could have meant and it is as unambiguous as the
    // swipe zones. If it scrolled and *then* ran out, that is overscroll, and a small bounce
    // past the end should not fling the drawer open.
    private var childScrolledThisGesture = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        // Drawer is showing and the finger moves down: close it before the list scrolls back.
        if (source != NestedScrollSource.Drag) return Offset.Zero
        if (state.progress <= 0f || available.y <= 0f) return Offset.Zero
        return Offset(0f, state.onDrag(available.y))
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        // Only leftover *upward* movement — i.e. the list had nothing left to give — opens.
        if (source != NestedScrollSource.Drag) return Offset.Zero
        if (consumed.y != 0f) childScrolledThisGesture = true
        if (available.y >= 0f) return Offset.Zero
        return Offset(0f, state.onDrag(available.y))
    }

    // Settling lives here and nowhere else: doing it in onPostFling as well would cancel
    // this animation and restart it at zero velocity, discarding the user's fling.
    override suspend fun onPreFling(available: Velocity): Velocity {
        val listNeverScrolled = !childScrolledThisGesture
        childScrolledThisGesture = false
        if (state.progress <= 0f) return Velocity.Zero
        state.settle(available.y, commitOnAnyUpwardDrag = listNeverScrolled)
        return available
    }
}

@Composable
fun rememberDrawerNestedScrollConnection(state: DrawerSwipeState): DrawerNestedScrollConnection =
    remember(state) { DrawerNestedScrollConnection(state) }

/**
 * Choreography, following Unlauncher's MotionScene: the home screen flies up and out while the
 * drawer rises into its place. The motion lives in the *translation* — both surfaces travel a
 * large fraction of the screen, which is what gives it the character of a list scrolling up
 * rather than one screen dissolving into another.
 *
 * An earlier attempt cut travel to 10% to stop the two reading as separate screens. That was
 * the wrong lever: the culprit was the drawer being an opaque panel over a translucent home
 * screen. With both now sharing one wallpaper and one scrim, the travel can be large again.
 *
 * Fades are staggered the way Unlauncher stagger theirs — via `android:alpha="-1"`, which
 * interpolates 1 → -1 so alpha hits 0 halfway rather than at the end. Home is gone by the
 * midpoint; the drawer commits over a short window after that, so the slide, not the fade,
 * is what the eye follows. No scaling: the reference has none, and it muddies the travel.
 *
 * Inputs are clamped — the settle spring may briefly leave 0..1, rendering must not follow it.
 */
object DrawerTransition {

    private const val HOME_TRAVEL = 0.75f
    private const val DRAWER_TRAVEL = 1.0f

    fun homeTranslationY(progress: Float, height: Float): Float =
        -progress.coerceIn(0f, 1f) * height * HOME_TRAVEL

    /** Gone by the midpoint, so only the first half of the climb is ever seen. */
    fun homeAlpha(progress: Float): Float = (1f - progress * 2f).coerceIn(0f, 1f)

    fun drawerTranslationY(progress: Float, height: Float): Float =
        (1f - progress.coerceIn(0f, 1f)) * height * DRAWER_TRAVEL

    /** Holds off until home is most of the way out, then commits over a short window. */
    fun drawerAlpha(progress: Float): Float = ((progress - 0.3f) / 0.3f).coerceIn(0f, 1f)
}
