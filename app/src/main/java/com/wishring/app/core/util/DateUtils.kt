package com.wishring.app.core.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Date utility functions for the app
 */
object DateUtils {
    
    /**
     * Standard date format for database storage
     */
    const val DATE_FORMAT_DB = "yyyy-MM-dd"
    
    /**
     * Display date format for UI
     */
    const val DATE_FORMAT_DISPLAY = "yyyy.MM.dd"
    
    /**
     * Full datetime format
     */
    const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"
    
    /**
     * Time format for display
     */
    const val TIME_FORMAT = "HH:mm"
    
    private val dbFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_DB)
    private val displayFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_DISPLAY)
    private val datetimeFormatter = DateTimeFormatter.ofPattern(DATETIME_FORMAT)
    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
    
    /**
     * Get today's date string in DB format
     * @return Today's date as "yyyy-MM-dd"
     */
    fun getTodayString(): String {
        return LocalDate.now().format(dbFormatter)
    }
    
    /**
     * Get today's date string in display format
     * @return Today's date as "yyyy.MM.dd"
     */
    fun getTodayDisplayString(): String {
        return LocalDate.now().format(displayFormatter)
    }
    
    /**
     * Convert date string to display format
     * @param dateString Date in DB format
     * @return Date in display format
     */
    fun toDisplayFormat(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, dbFormatter)
            date.format(displayFormatter)
        } catch (e: Exception) {
            dateString
        }
    }
    
    /**
     * Convert display format to DB format
     * @param displayString Date in display format
     * @return Date in DB format
     */
    fun toDbFormat(displayString: String): String {
        return try {
            val date = LocalDate.parse(displayString, displayFormatter)
            date.format(dbFormatter)
        } catch (e: Exception) {
            displayString
        }
    }
    
    /**
     * Get current timestamp
     * @return Current time in milliseconds
     */
    fun getCurrentTimestamp(): Long {
        return System.currentTimeMillis()
    }
    
    /**
     * Format timestamp to datetime string
     * @param timestamp Timestamp in milliseconds
     * @return Formatted datetime string
     */
    fun formatTimestamp(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return dateTime.format(datetimeFormatter)
    }
    
    /**
     * Format timestamp to time string
     * @param timestamp Timestamp in milliseconds
     * @return Formatted time string (HH:mm)
     */
    fun formatTime(timestamp: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestamp),
            ZoneId.systemDefault()
        )
        return dateTime.format(timeFormatter)
    }
    
    /**
     * Check if given date string is today
     * @param dateString Date in DB format
     * @return True if date is today
     */
    fun isToday(dateString: String): Boolean {
        return dateString == getTodayString()
    }
    
    /**
     * Get days difference between two dates
     * @param date1 First date in DB format
     * @param date2 Second date in DB format
     * @return Number of days between dates
     */
    fun getDaysDifference(date1: String, date2: String): Long {
        return try {
            val d1 = LocalDate.parse(date1, dbFormatter)
            val d2 = LocalDate.parse(date2, dbFormatter)
            java.time.temporal.ChronoUnit.DAYS.between(d1, d2)
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get formatted date relative to today
     * @param dateString Date in DB format
     * @return "오늘", "어제", "N일 전", or date string
     */
    fun getRelativeDateString(dateString: String): String {
        val daysDiff = getDaysDifference(dateString, getTodayString())
        
        return when (daysDiff) {
            0L -> "오늘"
            1L -> "어제"
            in 2..6 -> "${daysDiff}일 전"
            else -> toDisplayFormat(dateString)
        }
    }
    
    /**
     * Check if date is valid
     * @param dateString Date string to validate
     * @return True if valid date
     */
    fun isValidDate(dateString: String): Boolean {
        return try {
            LocalDate.parse(dateString, dbFormatter)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get date N days ago from today
     * @param days Number of days ago
     * @return Date string in DB format
     */
    fun getDateDaysAgo(days: Int): String {
        return LocalDate.now().minusDays(days.toLong()).format(dbFormatter)
    }
    
    /**
     * Get list of dates for last N days
     * @param days Number of days
     * @return List of date strings in DB format
     */
    fun getLastNDaysDates(days: Int): List<String> {
        return (0 until days).map { getDateDaysAgo(it) }.reversed()
    }
    
    /**
     * Convert Date to string
     * @param date Date object
     * @param pattern Format pattern
     * @return Formatted date string
     */
    fun dateToString(date: Date, pattern: String = DATE_FORMAT_DB): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Convert string to Date
     * @param dateString Date string
     * @param pattern Format pattern
     * @return Date object or null if parsing fails
     */
    fun stringToDate(dateString: String, pattern: String = DATE_FORMAT_DB): Date? {
        return try {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
}