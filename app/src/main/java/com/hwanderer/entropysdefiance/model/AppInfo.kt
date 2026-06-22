// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

/**
 * A launchable entry in the app drawer. This is a text-only launcher, so no app icon
 * is stored or rendered — loading every installed app's icon Drawable was pure wasted
 * work (and ran on the main thread), causing noticeable launcher lag.
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val launchIntent: String,
    val shortcutId: String? = null
) {
    val usageKey: String get() = if (shortcutId != null) "shortcut:$packageName:$shortcutId" else packageName
}
