// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * The app's shared motion vocabulary.
 *
 * Durations here were previously ad-hoc — 100, 150, 200, 3000ms and two different springs,
 * chosen per call site. Consistency of timing is most of what separates a surface that feels
 * designed from one that feels assembled, and it is far more perceptible than any individual
 * curve, so everything that moves should draw its timing from here.
 *
 * The app's identity is stillness: a mostly empty screen and large type. Motion is therefore
 * reserved for **changes of scope** — going deeper, collapsing to one task, stepping back to
 * see the whole ladder — and stays out of the resting state entirely.
 */
object Motion {

    /** Micro-feedback: an icon rotating, a control acknowledging a press. */
    const val QuickMillis = 120

    /** The default for a screen or scope change. */
    const val StandardMillis = 260

    /** Reserved for the few moments that should carry weight, like a horizon completing. */
    const val EmphasisMillis = 420

    /**
     * Everything decelerates into place and nothing arrives at speed: a fast start with a long
     * settle reads as calm, where a symmetric curve reads as mechanical.
     */
    val Easing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> quick(): FiniteAnimationSpec<T> = tween(QuickMillis, easing = Easing)

    fun <T> standard(): FiniteAnimationSpec<T> = tween(StandardMillis, easing = Easing)

    fun <T> emphasis(): FiniteAnimationSpec<T> = tween(EmphasisMillis, easing = Easing)

    /**
     * For surfaces the finger drives directly, which have to settle from a release velocity
     * rather than run for a fixed duration. Used by the app drawer.
     */
    val Settle: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 900f)

    /**
     * How far a surface travels while fading. Small on purpose — a long slide draws attention
     * to the transition itself rather than to what arrived.
     */
    const val SlideFraction = 0.06f
}
