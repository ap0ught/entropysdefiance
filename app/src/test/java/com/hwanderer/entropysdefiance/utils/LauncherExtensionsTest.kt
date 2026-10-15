// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.utils

import android.content.ActivityNotFoundException
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherExtensionsTest {

    private class TestLauncher(
        private val shouldThrow: Boolean
    ) : ActivityResultLauncher<String>() {
        var launchedInput: String? = null

        override fun launch(input: String?, options: ActivityOptionsCompat?) {
            if (shouldThrow) {
                throw ActivityNotFoundException("No activity found")
            }
            launchedInput = input
        }

        override fun unregister() {}

        override fun getContract(): ActivityResultContract<String, *> {
            throw UnsupportedOperationException()
        }
    }

    @Test
    fun `launchSafe catches ActivityNotFoundException and invokes onActivityNotFound`() {
        val launcher = TestLauncher(shouldThrow = true)
        var callbackInvoked = false

        val result = launcher.launchSafe(
            input = "test_input",
            onActivityNotFound = { callbackInvoked = true }
        )

        assertFalse(result)
        assertTrue(callbackInvoked)
    }

    @Test
    fun `launchSafe returns true when launch succeeds`() {
        val launcher = TestLauncher(shouldThrow = false)

        val result = launcher.launchSafe(input = "test_input")

        assertTrue(result)
        assertEquals("test_input", launcher.launchedInput)
    }
}
