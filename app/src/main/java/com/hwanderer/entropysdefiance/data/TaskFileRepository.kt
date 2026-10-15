// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

private const val TAG = "TaskFileRepository"
private const val DEFAULT_FILE_NAME = "tasks.json"

/**
 * Outcome of reading the task file. [Success] carries the parsed tasks (which may be
 * a legitimately empty list). [Failure] means the file could not be read or parsed
 * (missing file, lost permission, transient/partial content, corrupt JSON) and must
 * NOT be treated as "no tasks" — callers should keep their current state to avoid
 * wiping the user's tasks when a sync source is momentarily unavailable.
 */
sealed interface TaskReadResult {
    data class Success(val tasks: List<TaskData>) : TaskReadResult
    object Failure : TaskReadResult
}

/**
 * Repository for managing task persistence to JSON file.
 * This enables syncing tasks with external tools like Syncthing.
 */
class TaskFileRepository(private val context: Context) {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Get the default file location for tasks.json
     * Uses Documents/EntropysDefiance/tasks.json
     */
    private fun getDefaultFile(): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(documentsDir, "EntropysDefiance")
        appDir.mkdirs()
        return File(appDir, DEFAULT_FILE_NAME)
    }
    
    /**
     * Get the file based on configured path or default
     */
    fun getTaskFile(customPath: String): File {
        return if (customPath.isNotEmpty()) {
            File(customPath)
        } else {
            getDefaultFile()
        }
    }
    
    /**
     * Read tasks from JSON file.
     * Supports both file paths (older Android) and URIs (Android 13+).
     * Returns [TaskReadResult.Success] with the parsed tasks (possibly empty) only when
     * the file was read and parsed cleanly; otherwise [TaskReadResult.Failure] so callers
     * never mistake an unreadable file for "no tasks".
     */
    suspend fun readTasksFromFile(filePathOrUri: String): TaskReadResult = withContext(Dispatchers.IO) {
        try {
            if (filePathOrUri.startsWith("content://")) {
                // Android 13+: Handle as URI using SAF
                readTasksFromUri(Uri.parse(filePathOrUri))
            } else {
                // Older Android: Handle as file path
                readTasksFromFilePath(filePathOrUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from file: ${e.message}", e)
            TaskReadResult.Failure
        }
    }

    private suspend fun readTasksFromFilePath(filePath: String): TaskReadResult = withContext(Dispatchers.IO) {
        try {
            val file = getTaskFile(filePath)

            if (!file.exists()) {
                file.parentFile?.mkdirs()
                val emptyJson = json.encodeToString(TasksFileJson(version = 1, tasks = emptyList()))
                file.writeText(emptyJson)
                Log.d(TAG, "Initialized new tasks file at ${file.absolutePath}")
            }

            parseTasks(file.readText(), file.absolutePath)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from file path: ${e.message}", e)
            TaskReadResult.Failure
        }
    }

    private suspend fun readTasksFromUri(uri: Uri): TaskReadResult = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile == null || !documentFile.exists()) {
                Log.w(TAG, "Tasks file does not exist at URI (or access lost): $uri")
                return@withContext TaskReadResult.Failure
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return@withContext TaskReadResult.Failure
            }

            val jsonString = inputStream.use { it.bufferedReader().readText() }
            if (jsonString.isBlank()) {
                // If a newly created SAF document is 0 bytes, initialize it with valid JSON
                try {
                    val emptyJson = json.encodeToString(TasksFileJson(version = 1, tasks = emptyList()))
                    context.contentResolver.openOutputStream(uri, "w")?.use { os ->
                        os.write(emptyJson.toByteArray(Charsets.UTF_8))
                    }
                    return@withContext TaskReadResult.Success(emptyList())
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing blank URI file: ${e.message}", e)
                }
            }
            parseTasks(jsonString, uri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from URI: ${e.message}", e)
            TaskReadResult.Failure
        }
    }

    /**
     * Parse task JSON, distinguishing a genuinely-empty task list from an unreadable file.
     * A blank/0-byte payload is treated as a [TaskReadResult.Failure] rather than "no tasks":
     * normal writes always serialize an empty list as valid JSON with an empty array, so a
     * blank read indicates a partial/transient read (e.g. mid-sync) and must not wipe state.
     */
    private fun parseTasks(jsonString: String, source: String): TaskReadResult {
        if (jsonString.isBlank()) {
            Log.w(TAG, "Tasks file is blank, treating as read failure: $source")
            return TaskReadResult.Failure
        }
        return try {
            val tasksFile = json.decodeFromString<TasksFileJson>(jsonString)
            Log.d(TAG, "Successfully read ${tasksFile.tasks.size} tasks from $source")
            TaskReadResult.Success(tasksFile.tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tasks JSON from $source: ${e.message}", e)
            TaskReadResult.Failure
        }
    }
    
    /**
     * Write tasks to JSON file
     * Supports both file paths (older Android) and URIs (Android 13+)
     */
    suspend fun writeTasksToFile(tasks: List<TaskData>, filePathOrUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePathOrUri.startsWith("content://")) {
                // Android 13+: Handle as URI using SAF
                writeTasksToUri(tasks, Uri.parse(filePathOrUri))
            } else {
                // Older Android: Handle as file path
                writeTasksToFilePath(tasks, filePathOrUri)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tasks to file: ${e.message}", e)
            false
        }
    }

    private suspend fun writeTasksToFilePath(tasks: List<TaskData>, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getTaskFile(filePath)

            // Create parent directories if they don't exist
            file.parentFile?.mkdirs()

            val tasksFile = TasksFileJson(
                version = 1,
                tasks = tasks
            )

            val jsonString = json.encodeToString(tasksFile)

            // Write atomically by writing to temp file then renaming
            val tempFile = File(file.parent, "${file.name}.tmp")
            
            // Write with proper flushing and syncing
            tempFile.outputStream().use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync() // Force write to disk before rename
            }

            // Atomic rename
            if (tempFile.renameTo(file)) {
                Log.d(TAG, "Successfully wrote ${tasks.size} tasks to ${file.absolutePath}")
                true
            } else {
                Log.e(TAG, "Failed to rename temp file to ${file.absolutePath}")
                tempFile.delete()
                false
            }
        } catch (e: IOException) {
            Log.e(TAG, "IO Error writing tasks to file path: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tasks to file path: ${e.message}", e)
            false
        }
    }

    private suspend fun writeTasksToUri(tasks: List<TaskData>, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val tasksFile = TasksFileJson(
                version = 1,
                tasks = tasks
            )

            val jsonString = json.encodeToString(tasksFile)

            val outputStream = context.contentResolver.openOutputStream(uri, "wt") // "wt" = write, truncate
            if (outputStream == null) {
                Log.e(TAG, "Could not open output stream for URI: $uri")
                return@withContext false
            }

            // Write with proper flushing
            outputStream.use { os ->
                os.write(jsonString.toByteArray(Charsets.UTF_8))
                os.flush()
                // Note: FileDescriptor.sync() may not be available for URIs
                // The 'wt' mode and flush() should be sufficient
            }

            Log.d(TAG, "Successfully wrote ${tasks.size} tasks to URI: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error writing tasks to URI: ${e.message}", e)
            false
        }
    }
    
    /**
     * Check if the tasks file exists
     * Supports both file paths and URIs
     */
    fun fileExists(filePathOrUri: String): Boolean {
        return if (filePathOrUri.startsWith("content://")) {
            // Android 13+: Check URI using SAF
            val uri = Uri.parse(filePathOrUri)
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            documentFile?.exists() == true
        } else {
            // Older Android: Check file path
            getTaskFile(filePathOrUri).exists()
        }
    }

    /**
     * Get the display path of the tasks file
     * For URIs, returns a simplified representation
     */
    fun getDisplayPath(filePathOrUri: String): String {
        return if (filePathOrUri.startsWith("content://")) {
            // Android 13+: Show simplified URI representation
            val uri = Uri.parse(filePathOrUri)
            val path = uri.path
            if (path != null && path.contains(":")) {
                val parts = path.split(":")
                if (parts.size > 1) {
                    "📁 /storage/emulated/0/" + parts[1]
                } else {
                    "📁 Selected file (Android 13+)"
                }
            } else {
                "📁 Selected file (Android 13+)"
            }
        } else {
            // Older Android: Show file path
            getTaskFile(filePathOrUri).absolutePath
        }
    }
    
    /**
     * Create an empty tasks file if it doesn't exist
     * For URIs, this just writes an empty file if the URI is accessible
     */
    suspend fun createEmptyFileIfNeeded(filePathOrUri: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePathOrUri.startsWith("content://")) {
                // Android 13+: For URIs, just try to write an empty file
                // The file should already exist since the user selected it
                if (!fileExists(filePathOrUri)) {
                    writeTasksToFile(emptyList(), filePathOrUri)
                } else {
                    true
                }
            } else {
                // Older Android: Create file if needed
                val file = getTaskFile(filePathOrUri)
                if (!file.exists()) {
                    file.parentFile?.mkdirs()
                    writeTasksToFile(emptyList(), filePathOrUri)
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating empty file: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get the last modified time of the tasks file
     */
    fun getLastModified(filePath: String): Long {
        return getTaskFile(filePath).lastModified()
    }
}
