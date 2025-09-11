package com.example.entropysdefiance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon,
    @PrimaryKey(autoGenerate = true)
    val autoId: Long? = null,
    val createdAt: LocalDateTime? = null,
    val isCompleted: Boolean = false
) {
    fun toTask() = Task(id, title, timeHorizon)
} 