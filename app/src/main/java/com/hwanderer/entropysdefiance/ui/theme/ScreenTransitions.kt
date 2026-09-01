// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.hwanderer.entropysdefiance.model.TimeHorizon

/**
 * How a screen relates to the one it replaces.
 *
 * The app already has a spatial grammar for scope — Zen Mode collapses to a single task,
 * Alignment steps back to see the whole ladder of horizons, settings go deeper — and until
 * now none of it was expressed in motion; every screen simply cut to the next. These kinds
 * let the transition carry the same meaning the interaction already has.
 */
enum class ScreenMotion {
    /** Ordinary push and pop, ordered by [AppScreen.depth]. */
    Stack,

    /** Zooming in on one thing. The camera flies toward the screen. */
    Inward,

    /** Pulling back to see everything. The camera retreats. */
    Outward
}

/**
 * Every full-screen state, with the depth used to tell a push from a pop.
 *
 * Depths only need to be ordered relative to each other, not exact.
 */
enum class AppScreen(val depth: Int, val motion: ScreenMotion = ScreenMotion.Stack) {
    Home(0),

    AddTask(1),
    ZenDurationDialog(1),
    Settings(1),

    CalendarSettings(2),
    AppearanceSettings(2),
    FreeTimeSettings(2),
    FileSyncSettings(2),
    PermissionsSettings(2),
    HiddenAppsSettings(2),
    ZenModeSettings(2),
    BackupSettings(2),
    Statistics(2),

    CalendarSelection(3),
    CalculationCalendarSelection(3),
    ColorPaletteSelection(3),

    /** Collapse to a single task. */
    ZenMode(0, ScreenMotion.Inward),

    /** Step back through This Life → Today. */
    Alignment(0, ScreenMotion.Outward)
}

private fun ScreenMotion.reversed(): ScreenMotion = when (this) {
    ScreenMotion.Stack -> ScreenMotion.Stack
    ScreenMotion.Inward -> ScreenMotion.Outward
    ScreenMotion.Outward -> ScreenMotion.Inward
}

/**
 * The transition to play when [from] is replaced by [to].
 *
 * Zen Mode and Alignment impose their own metaphor whichever screen they arrive from or
 * return to; everything else is an ordinary push or pop ordered by depth.
 */
fun screenTransition(from: AppScreen, to: AppScreen): ContentTransform {
    val motion = when {
        to.motion != ScreenMotion.Stack -> to.motion
        from.motion != ScreenMotion.Stack -> from.motion.reversed()
        else -> ScreenMotion.Stack
    }

    val transform = when (motion) {
        // Flying in: what is leaving grows past the camera, what arrives rises to meet it.
        ScreenMotion.Inward ->
            (fadeIn(Motion.standard()) + scaleIn(Motion.standard(), initialScale = 0.92f)) togetherWith
                (fadeOut(Motion.standard()) + scaleOut(Motion.standard(), targetScale = 1.08f))

        // Pulling back: the reverse, so returning from Zen Mode undoes it exactly.
        ScreenMotion.Outward ->
            (fadeIn(Motion.standard()) + scaleIn(Motion.standard(), initialScale = 1.08f)) togetherWith
                (fadeOut(Motion.standard()) + scaleOut(Motion.standard(), targetScale = 0.92f))

        ScreenMotion.Stack -> {
            // A short travel, in the direction of the push. Longer slides draw attention to
            // the transition itself rather than to what arrived.
            val forward = to.depth >= from.depth
            val sign = if (forward) 1 else -1
            (
                fadeIn(Motion.standard()) +
                    slideInVertically(Motion.standard()) {
                        (sign * it * Motion.SlideFraction).toInt()
                    }
                ) togetherWith (
                fadeOut(Motion.standard()) +
                    slideOutVertically(Motion.standard()) {
                        (-sign * it * Motion.SlideFraction * 0.5f).toInt()
                    }
                )
        }
    }

    // No SizeTransform: full-screen content never changes size, so the default clip is both
    // harmless and correct — nothing should spill outside the screen mid-slide.
    return transform
}


/**
 * Which horizon the home screen is showing, and why.
 *
 * The distinction matters more than it looks. The home screen has no horizon navigation by
 * design — it shows the smallest horizon that still has work in it, and peek is a
 * press-and-hold that reverts on release, so lingering in a wider view is structurally
 * impossible. A peek and a real advance are therefore *different events* that happen to
 * change the same value, and they should not look alike.
 */
data class HorizonView(val horizon: TimeHorizon, val isPeek: Boolean)

/**
 * A peek is a pure cross-dissolve: nothing moves, because you are looking *through* to
 * another scope rather than travelling to it, and you will be back in a moment.
 *
 * An advance is the opposite — you finished everything in a horizon and the launcher has
 * moved you out to the next one. That is the whole point of the app, and it was previously
 * indistinguishable from a repaint. It gets the emphasis duration and a small rise, so the
 * change registers as something earned rather than something that merely happened.
 *
 * The rise stays small, and there is no flourish beyond it: a launcher whose identity is
 * stillness should mark its best moment with weight, not with confetti.
 */
fun horizonTransition(from: HorizonView, to: HorizonView): ContentTransform {
    val isPeekChange = from.isPeek || to.isPeek
    if (isPeekChange) {
        // A glance: both states are the same content seen from a different scope, so
        // dissolving them through each other is exactly right.
        return fadeIn(Motion.quick()) togetherWith fadeOut(Motion.quick())
    }

    // An advance. Staged rather than simultaneous: by the time the horizon flips, the list
    // being left behind is already empty — the last task was removed first — so an overlapping
    // cross-fade has nothing to fade out and the new horizon merely materialises over blank
    // space. Letting the old state clear first gives the moment a beat to land in.
    val outMillis = (Motion.EmphasisMillis * Motion.FadeThroughOutFraction).toInt()
    val inMillis = Motion.EmphasisMillis - outMillis
    return (
        fadeIn(tween(inMillis, delayMillis = outMillis, easing = Motion.Easing)) +
            slideInVertically(
                tween(inMillis, delayMillis = outMillis, easing = Motion.Easing)
            ) { (it * Motion.EmphasisSlideFraction).toInt() }
        ) togetherWith fadeOut(tween(outMillis, easing = Motion.Easing))
}
