package com.example.entropysdefiance.data

import com.example.entropysdefiance.model.TimeHorizon
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
 * JSON representation of a task for serialization
 */
@Serializable
data class TaskJson(
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
    companion object {
        /**
         * Convert TaskEntity to TaskJson
         */
        fun fromEntity(entity: TaskEntity): TaskJson {
            return TaskJson(
                id = entity.id,
                title = entity.title,
                timeHorizon = entity.timeHorizon.name,
                autoId = entity.autoId,
                createdAt = entity.createdAt,
                isCompleted = entity.isCompleted,
                isDeadlineTask = entity.isDeadlineTask,
                deadline = entity.deadline,
                daysNeeded = entity.daysNeeded,
                doDate = entity.doDate
            )
        }
    }
    
    /**
     * Convert TaskJson to TaskEntity
     */
    fun toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            timeHorizon = TimeHorizon.valueOf(timeHorizon),
            autoId = autoId,
            createdAt = createdAt,
            isCompleted = isCompleted,
            isDeadlineTask = isDeadlineTask,
            deadline = deadline,
            daysNeeded = daysNeeded,
            doDate = doDate
        )
    }
}

/**
 * Root JSON structure for the tasks file
 */
@Serializable
data class TasksFileJson(
    val version: Int = 1,
    val tasks: List<TaskJson> = emptyList()
)
