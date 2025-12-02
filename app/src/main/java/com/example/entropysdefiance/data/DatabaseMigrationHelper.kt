package com.example.entropysdefiance.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.example.entropysdefiance.model.TimeHorizon
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

private const val TAG = "DatabaseMigrationHelper"

/**
 * Helper to migrate tasks from the old Room database to the new JSON file format.
 * This reads directly from the SQLite database file without requiring Room dependencies.
 */
object DatabaseMigrationHelper {
    
    /**
     * Check if the old Room database exists
     */
    fun hasOldDatabase(context: Context): Boolean {
        val dbPath = context.getDatabasePath("task_database")
        return dbPath.exists()
    }
    
    /**
     * Migrate tasks from the old Room database to TaskData list
     * Returns null if database doesn't exist or migration fails
     */
    fun migrateFromOldDatabase(context: Context): List<TaskData>? {
        val dbPath = context.getDatabasePath("task_database")
        if (!dbPath.exists()) {
            Log.d(TAG, "Old database not found, skipping migration")
            return null
        }
        
        return try {
            Log.d(TAG, "Starting migration from old database: ${dbPath.absolutePath}")
            val db = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            val tasks = mutableListOf<TaskData>()
            val cursor = db.rawQuery("SELECT * FROM tasks", null)
            
            cursor.use {
                val idIndex = it.getColumnIndex("id")
                val titleIndex = it.getColumnIndex("title")
                val timeHorizonIndex = it.getColumnIndex("timeHorizon")
                val autoIdIndex = it.getColumnIndex("autoId")
                val createdAtIndex = it.getColumnIndex("createdAt")
                val isCompletedIndex = it.getColumnIndex("isCompleted")
                val isDeadlineTaskIndex = it.getColumnIndex("isDeadlineTask")
                val deadlineIndex = it.getColumnIndex("deadline")
                val daysNeededIndex = it.getColumnIndex("daysNeeded")
                val doDateIndex = it.getColumnIndex("doDate")
                
                while (it.moveToNext()) {
                    try {
                        val id = if (idIndex >= 0) it.getString(idIndex) else java.util.UUID.randomUUID().toString()
                        val title = if (titleIndex >= 0) it.getString(titleIndex) else "Untitled Task"
                        val timeHorizonStr = if (timeHorizonIndex >= 0) it.getString(timeHorizonIndex) else "Today"
                        val autoId = if (autoIdIndex >= 0 && !it.isNull(autoIdIndex)) it.getLong(autoIdIndex) else null
                        val createdAtStr = if (createdAtIndex >= 0 && !it.isNull(createdAtIndex)) it.getString(createdAtIndex) else null
                        val isCompleted = if (isCompletedIndex >= 0) it.getInt(isCompletedIndex) == 1 else false
                        val isDeadlineTask = if (isDeadlineTaskIndex >= 0) it.getInt(isDeadlineTaskIndex) == 1 else false
                        val deadlineStr = if (deadlineIndex >= 0 && !it.isNull(deadlineIndex)) it.getString(deadlineIndex) else null
                        val daysNeeded = if (daysNeededIndex >= 0 && !it.isNull(daysNeededIndex)) it.getInt(daysNeededIndex) else null
                        val doDateStr = if (doDateIndex >= 0 && !it.isNull(doDateIndex)) it.getString(doDateIndex) else null
                        
                        val taskData = TaskData(
                            id = id,
                            title = title,
                            timeHorizon = timeHorizonStr,
                            autoId = autoId,
                            createdAt = createdAtStr?.let { str -> 
                                try {
                                    LocalDateTime.parse(str)
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            isCompleted = isCompleted,
                            isDeadlineTask = isDeadlineTask,
                            deadline = deadlineStr?.let { str ->
                                try {
                                    LocalDate.parse(str)
                                } catch (e: Exception) {
                                    null
                                }
                            },
                            daysNeeded = daysNeeded,
                            doDate = doDateStr?.let { str ->
                                try {
                                    LocalDate.parse(str)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        )
                        
                        tasks.add(taskData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing task row: ${e.message}", e)
                        // Continue with next row
                    }
                }
            }
            
            db.close()
            
            Log.d(TAG, "Successfully migrated ${tasks.size} tasks from old database")
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Error migrating from old database: ${e.message}", e)
            null
        }
    }
    
    /**
     * Delete the old Room database after successful migration
     */
    fun deleteOldDatabase(context: Context): Boolean {
        return try {
            val dbPath = context.getDatabasePath("task_database")
            val deleted = dbPath.delete()
            
            // Also delete the journal files
            File(dbPath.absolutePath + "-shm").delete()
            File(dbPath.absolutePath + "-wal").delete()
            File(dbPath.absolutePath + "-journal").delete()
            
            Log.d(TAG, "Old database deleted: $deleted")
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting old database: ${e.message}", e)
            false
        }
    }
}
