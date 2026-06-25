// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Guards the regression behind the app-drawer crash: a package exposing multiple launcher
 * activities must yield distinct list keys (so a LazyColumn never sees duplicate keys), while
 * the persisted usage key stays per-package so existing usage counters survive.
 */
class AppInfoTest {

    @Test
    fun `usageKey is the package name for a plain app`() {
        val app = AppInfo(
            packageName = "com.example.app",
            appName = "Example",
            launchIntent = "com.example.app.MainActivity"
        )

        assertEquals("com.example.app", app.usageKey)
    }

    @Test
    fun `two launcher activities of the same package share a usageKey but get distinct listKeys`() {
        val first = AppInfo(
            packageName = "com.example.app",
            appName = "Example",
            launchIntent = "com.example.app.ActivityA"
        )
        val second = AppInfo(
            packageName = "com.example.app",
            appName = "Example",
            launchIntent = "com.example.app.ActivityB"
        )

        // Usage counts stay per-package.
        assertEquals(first.usageKey, second.usageKey)
        // List keys must differ so a LazyColumn does not crash on duplicate keys.
        assertNotEquals(first.listKey, second.listKey)
    }

    @Test
    fun `shortcut uses the same stable key for both usage and list rendering`() {
        val shortcut = AppInfo(
            packageName = "com.example.pwa",
            appName = "Web App",
            launchIntent = "",
            shortcutId = "shortcut-1"
        )

        assertEquals("shortcut:com.example.pwa:shortcut-1", shortcut.usageKey)
        assertEquals(shortcut.usageKey, shortcut.listKey)
    }
}
