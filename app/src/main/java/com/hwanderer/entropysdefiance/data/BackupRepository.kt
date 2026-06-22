// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BackupRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "BackupRepository"
    }
    
    suspend fun exportTasksToCsv(tasks: List<TaskData>, outputStream: android.os.ParcelFileDescriptor) {
        withContext(Dispatchers.IO) {
            try {
                val writer = OutputStreamWriter(outputStream.fileDescriptor.let { 
                    java.io.FileOutputStream(it) 
                })
                
                // Write CSV header
                writer.write("ID,Title,TimeHorizon,CreatedAt,IsCompleted\n")
                
                // Write task data
                tasks.forEach { task ->
                    val createdAt = task.createdAt?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: ""
                    val autoId = task.autoId ?: 0L
                    val isCompleted = task.isCompleted
                    writer.write("$autoId,\"${task.title}\",${task.timeHorizon},$createdAt,${if (isCompleted) "1" else "0"}\n")
                }
                
                writer.close()
                Log.d(TAG, "Successfully exported ${tasks.size} tasks to CSV")
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting tasks to CSV", e)
                throw e
            }
        }
    }
    
    suspend fun importTasksFromCsv(inputStream: java.io.InputStream): List<TaskData> {
        return withContext(Dispatchers.IO) {
            try {
                val tasks = mutableListOf<TaskData>()
                val reader = inputStream.bufferedReader()
                val lines = reader.readLines()
                
                // Skip header line
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val parts = parseCsvLine(line)
                    
                    if (parts.size >= 5) {
                        val task = TaskData(
                            id = java.util.UUID.randomUUID().toString(),
                            title = parts[1],
                            timeHorizon = parts[2],
                            autoId = null,
                            createdAt = if (parts[3].isNotEmpty()) {
                                LocalDateTime.parse(parts[3], DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            } else LocalDateTime.now(),
                            isCompleted = parts[4] == "1"
                        )
                        tasks.add(task)
                    }
                }
                
                reader.close()
                Log.d(TAG, "Successfully imported ${tasks.size} tasks from CSV")
                tasks
            } catch (e: Exception) {
                Log.e(TAG, "Error importing tasks from CSV", e)
                throw e
            }
        }
    }
    
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (i in line.indices) {
            val char = line[i]
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}
