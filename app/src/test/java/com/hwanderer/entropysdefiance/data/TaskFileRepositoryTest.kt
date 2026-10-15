// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27])
class TaskFileRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `reading zero byte file initializes valid empty task JSON`() = runBlocking {
        val file = tempFolder.newFile("tasks.json")
        val repository = TaskFileRepository(ApplicationProvider.getApplicationContext<Context>())

        val result = repository.readTasksFromFile(file.absolutePath)

        assertEquals(TaskReadResult.Success(emptyList()), result)
        assertTrue(file.readText().contains("\"tasks\": []"))
    }
}
