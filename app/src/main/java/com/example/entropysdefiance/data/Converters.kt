package com.example.entropysdefiance.data

import androidx.room.TypeConverter
import com.example.entropysdefiance.model.TimeHorizon
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    @TypeConverter
    fun fromTimeHorizon(value: TimeHorizon): String {
        return value.name
    }

    @TypeConverter
    fun toTimeHorizon(value: String): TimeHorizon {
        return TimeHorizon.valueOf(value)
    }
    
    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
    }
} 