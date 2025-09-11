package com.example.entropysdefiance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    fun getPendingTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun markTaskAsCompleted(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    suspend fun getCompletedTaskCount(): Int
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    suspend fun getPendingTaskCount(): Int
    
    @Query("SELECT MIN(createdAt) FROM tasks")
    suspend fun getOldestTaskDate(): LocalDateTime?
    
    @Query("SELECT * FROM tasks ORDER BY autoId DESC LIMIT 1")
    suspend fun getLastTask(): TaskEntity?
} 