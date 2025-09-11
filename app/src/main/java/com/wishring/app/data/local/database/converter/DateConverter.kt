package com.wishring.app.data.local.database.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database
 * Handles conversion between Date and Long for database storage
 */
class DateConverter {
    
    /**
     * Convert timestamp to Date
     * @param value Timestamp in milliseconds
     * @return Date object or null
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Convert Date to timestamp
     * @param date Date object
     * @return Timestamp in milliseconds or null
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}