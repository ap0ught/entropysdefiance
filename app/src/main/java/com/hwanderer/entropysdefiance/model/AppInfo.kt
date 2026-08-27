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
    val shortcutId: String? = null,
    /**
     * The label the system reports, kept even when [appName] has been overridden by a user
     * rename — searching for what an app is actually called must keep working.
     */
    val originalName: String? = null
) {
    /**
     * Stable key for persisted usage counts (SharedPreferences). Intentionally per-package
     * rather than per-activity: launching any launcher activity of a package counts toward the
     * same app. Changing this format would orphan existing counters, so it must stay stable.
     */
    val usageKey: String get() = if (shortcutId != null) "shortcut:$packageName:$shortcutId" else packageName

    /**
     * Genuinely unique key for list rendering. A single package can expose multiple launcher
     * activities — each a separate AppInfo with the same packageName — so keying a LazyColumn by
     * [usageKey] alone throws on duplicate keys (and crashes the launcher). Folding in the launch
     * activity name keeps every drawer entry distinct without disturbing the usage-count key.
     */
    val listKey: String get() = if (shortcutId != null) "shortcut:$packageName:$shortcutId" else "$packageName/$launchIntent"
}
