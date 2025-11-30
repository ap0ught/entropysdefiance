package com.example.entropysdefiance.utils

import android.os.FileObserver
import android.util.Log
import java.io.File

private const val TAG = "TaskFileObserver"

/**
 * FileObserver to detect external changes to the tasks.json file.
 * This allows the app to reload tasks when they're edited externally
 * (e.g., via Syncthing or a text editor on laptop).
 */
class TaskFileObserver(
    private val file: File,
    private val onFileChanged: () -> Unit
) : FileObserver(file.parentFile ?: file, CLOSE_WRITE or MOVED_TO) {
    
    private var lastModified: Long = file.lastModified()
    
    override fun onEvent(event: Int, path: String?) {
        // Only react to changes to our specific file
        if (path != null && path == file.name) {
            when (event) {
                CLOSE_WRITE, MOVED_TO -> {
                    // Check if the file was actually modified (debounce)
                    val currentModified = file.lastModified()
                    if (currentModified > lastModified) {
                        lastModified = currentModified
                        Log.d(TAG, "Tasks file changed externally: ${file.absolutePath}")
                        onFileChanged()
                    }
                }
            }
        }
    }
    
    /**
     * Start watching the file for changes
     */
    fun startWatching(filePath: File) {
        try {
            startWatching()
            lastModified = filePath.lastModified()
            Log.d(TAG, "Started watching file: ${filePath.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting file observer: ${e.message}", e)
        }
    }
    
    /**
     * Stop watching the file
     */
    fun stopWatchingFile() {
        try {
            stopWatching()
            Log.d(TAG, "Stopped watching file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping file observer: ${e.message}", e)
        }
    }
}
