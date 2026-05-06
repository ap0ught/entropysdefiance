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
     * Read tasks from JSON file
     * Supports both file paths (older Android) and URIs (Android 13+)
     * Returns empty list if file doesn't exist or is corrupted
     */
    suspend fun readTasksFromFile(filePathOrUri: String): List<TaskData> = withContext(Dispatchers.IO) {
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
            emptyList()
        }
    }

    private suspend fun readTasksFromFilePath(filePath: String): List<TaskData> = withContext(Dispatchers.IO) {
        try {
            val file = getTaskFile(filePath)

            if (!file.exists()) {
                Log.d(TAG, "Tasks file does not exist: ${file.absolutePath}")
                return@withContext emptyList()
            }

            val jsonString = file.readText()
            if (jsonString.isBlank()) {
                Log.d(TAG, "Tasks file is empty: ${file.absolutePath}")
                return@withContext emptyList()
            }

            val tasksFile = json.decodeFromString<TasksFileJson>(jsonString)
            Log.d(TAG, "Successfully read ${tasksFile.tasks.size} tasks from ${file.absolutePath}")

            tasksFile.tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from file path: ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun readTasksFromUri(uri: Uri): List<TaskData> = withContext(Dispatchers.IO) {
        try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            if (documentFile == null || !documentFile.exists()) {
                Log.d(TAG, "Tasks file does not exist at URI: $uri")
                return@withContext emptyList()
            }

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "Could not open input stream for URI: $uri")
                return@withContext emptyList()
            }

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            if (jsonString.isBlank()) {
                Log.d(TAG, "Tasks file is empty at URI: $uri")
                return@withContext emptyList()
            }

            val tasksFile = json.decodeFromString<TasksFileJson>(jsonString)
            Log.d(TAG, "Successfully read ${tasksFile.tasks.size} tasks from URI: $uri")

            tasksFile.tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error reading tasks from URI: ${e.message}", e)
            emptyList()
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
