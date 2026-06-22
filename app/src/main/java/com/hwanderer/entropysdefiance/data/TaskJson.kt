// Copyright (c) 2024 Jonas Freiburghaus
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
// SPDX-License-Identifier: MPL-2.0

package com.hwanderer.entropysdefiance.data

import com.hwanderer.entropysdefiance.model.Task
import com.hwanderer.entropysdefiance.model.TimeHorizon
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Custom serializer for LocalDateTime
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }
    
    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}

/**
 * Custom serializer for LocalDate
 */
object LocalDateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }
    
    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}

/**
 * Internal representation of a task for file storage
 */
@Serializable
data class TaskData(
    val id: String,
    val title: String,
    val timeHorizon: String,
    val autoId: Long? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val isDeadlineTask: Boolean = false,
    @Serializable(with = LocalDateSerializer::class)
    val deadline: LocalDate? = null,
    val daysNeeded: Int? = null,
    @Serializable(with = LocalDateSerializer::class)
    val doDate: LocalDate? = null
) {
    /**
     * Convert TaskData to Task model
     */
    fun toTask(): Task {
        return Task(
            id = id,
            title = title,
            timeHorizon = TimeHorizon.valueOf(timeHorizon),
            isDeadlineTask = isDeadlineTask,
            deadline = deadline,
            doDate = doDate
        )
    }
    
    companion object {
        /**
         * Convert Task model to TaskData
         */
        fun fromTask(task: Task, autoId: Long?, createdAt: LocalDateTime?, isCompleted: Boolean, daysNeeded: Int?): TaskData {
            return TaskData(
                id = task.id,
                title = task.title,
                timeHorizon = task.timeHorizon.name,
                autoId = autoId,
                createdAt = createdAt,
                isCompleted = isCompleted,
                isDeadlineTask = task.isDeadlineTask,
                deadline = task.deadline,
                daysNeeded = daysNeeded,
                doDate = task.doDate
            )
        }
    }
}

// Alias for backward compatibility with existing code
typealias TaskJson = TaskData

/**
 * Root JSON structure for the tasks file
 */
@Serializable
data class TasksFileJson(
    val version: Int = 1,
    val tasks: List<TaskData> = emptyList()
)
