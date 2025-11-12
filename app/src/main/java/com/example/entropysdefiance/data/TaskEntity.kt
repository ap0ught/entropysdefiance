package com.example.entropysdefiance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.entropysdefiance.model.Task
import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime
import java.time.LocalDate

@Entity(tableName = "tasks")
data class TaskEntity(
    val id: String,
    val title: String,
    val timeHorizon: TimeHorizon,
    @PrimaryKey(autoGenerate = true)
    val autoId: Long? = null,
    val createdAt: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val isDeadlineTask: Boolean = false,
    val deadline: LocalDate? = null,
    val daysNeeded: Int? = null,
    val doDate: LocalDate? = null
) {
    fun toTask() = Task(
        id = id,
        title = title,
        timeHorizon = timeHorizon,
        isDeadlineTask = isDeadlineTask,
        deadline = deadline,
        doDate = doDate
    )
} 