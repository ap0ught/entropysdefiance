package com.example.entropysdefiance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon
) {
    fun toTask() = Task(id, title, timeHorizon)
} 