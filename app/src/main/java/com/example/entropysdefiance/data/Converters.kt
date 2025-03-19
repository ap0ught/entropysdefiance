package com.example.entropysdefiance.data

import androidx.room.TypeConverter
import com.example.entropysdefiance.model.TimeHorizon

class Converters {
    @TypeConverter
    fun fromTimeHorizon(value: TimeHorizon): String {
        return value.name
    }

    @TypeConverter
    fun toTimeHorizon(value: String): TimeHorizon {
        return TimeHorizon.valueOf(value)
    }
} 