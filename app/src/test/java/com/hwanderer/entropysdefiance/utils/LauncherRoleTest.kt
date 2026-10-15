// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LauncherRoleTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun `buildDefaultLauncherIntent on API 28 returns home settings without NoClassDefFoundError`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = buildDefaultLauncherIntent(context)
        assertNotNull(intent)
        assertEquals(Settings.ACTION_HOME_SETTINGS, intent.action)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `buildDefaultLauncherIntent on API 29 executes role manager check safely`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val intent = buildDefaultLauncherIntent(context)
        assertNotNull(intent)
    }
}
