package com.wishring.app.data.repository

import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.database.entity.ResetType
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.model.ResetStatistics
import com.wishring.app.domain.model.getSummaryStatistics
import com.wishring.app.domain.model.toEntity
import com.wishring.app.domain.repository.ResetFrequencyAnalysis
import com.wishring.app.domain.repository.ResetImpactAnalysis
import com.wishring.app.domain.repository.ResetLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ResetLogRepository
 * Manages ResetLog data operations using Room DAO directly
 */
@Singleton
class ResetLogRepositoryImpl @Inject constructor(
    private val resetLogDao: ResetLogDao
) : ResetLogRepository {
    
    override fun getAllResetLogs(): Flow<List<ResetLog>> {
        return resetLogDao.getAllLogs().map { entities ->
            entities.map { ResetLog.fromEntity(it) }
        }
    }
    
    override suspend fun getResetLogsByDate(date: String): List<ResetLog> {
        return resetLogDao.getLogsByDate(date).map {
            ResetLog.fromEntity(it)
        }
    }
    
    override suspend fun getResetLogsBetween(startDate: String, endDate: String): List<ResetLog> {
        return resetLogDao.getLogsBetween(startDate, endDate).map {
            ResetLog.fromEntity(it)
        }
    }
    
    override suspend fun getRecentResetLogs(limit: Int): List<ResetLog> {
        return resetLogDao.getRecentLogs(limit).map {
            ResetLog.fromEntity(it)
        }
    }
    
    override fun getResetLogsByType(resetType: ResetType): Flow<List<ResetLog>> {
        return resetLogDao.getLogsByType(resetType.name).map { entities ->
            entities.map { ResetLog.fromEntity(it) }
        }
    }
    
    override suspend fun getTodayResetLogs(): List<ResetLog> {
        return resetLogDao.getTodayLogs().map {
            ResetLog.fromEntity(it)
        }
    }
    
    override fun getSignificantResetLogs(): Flow<List<ResetLog>> {
        return resetLogDao.getSignificantLogs().map { entities ->
            entities.map { ResetLog.fromEntity(it) }
        }
    }
    
    override suspend fun getLastReset(): ResetLog? {
        return resetLogDao.getLastReset()?.let {
            ResetLog.fromEntity(it)
        }
    }
    
    override suspend fun getTotalLostCount(): Int {
        return resetLogDao.getTotalLostCount() ?: 0
    }
    
    override suspend fun getTodayResetCount(): Int {
        return resetLogDao.getTodayResetCount()
    }
    
    override suspend fun getResetCountByType(resetType: ResetType): Int {
        return resetLogDao.getResetCountByType(resetType.name)
    }
    
    override suspend fun hasResetToday(): Boolean {
        return resetLogDao.hasResetToday()
    }
    
    override suspend fun getResetStatistics(): ResetStatistics {
        val stats = resetLogDao.getResetStatistics()
        return ResetStatistics(
            totalResets = stats.sumOf { it.count },
            totalLostCount = getTotalLostCount(),
            manualResets = stats.find { it.resetType == ResetType.MANUAL.name }?.count ?: 0,
            autoResets = stats.find { it.resetType == ResetType.AUTO.name }?.count ?: 0,
            dailyResets = stats.find { it.resetType == ResetType.MIDNIGHT.name }?.count ?: 0,
            emergencyResets = stats.find { it.resetType == ResetType.ERROR.name }?.count ?: 0,
            averageLostCount = 0 // Will be calculated below
        ).let { baseStats ->
            val allLogs = getRecentResetLogs(Int.MAX_VALUE)
            val avgLost = if (allLogs.isNotEmpty()) {
                allLogs.sumOf { it.countBeforeReset } / allLogs.size
            } else 0
            baseStats.copy(averageLostCount = avgLost)
        }
    }
    
    override suspend fun getResetStatisticsForPeriod(
        startDate: String,
        endDate: String
    ): ResetStatistics {
        val logs = getResetLogsBetween(startDate, endDate)
        return logs.getSummaryStatistics()
    }
    
    override suspend fun saveResetLog(resetLog: ResetLog): ResetLog {
        val id = resetLogDao.insert(resetLog.toEntity())
        return resetLog.copy(id = id)
    }
    
    override suspend fun logManualReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String,
        reason: String?
    ): ResetLog {
        val resetLog = ResetLog(
            date = DateUtils.getTodayString(),
            resetTime = DateUtils.getCurrentTimestamp(),
            countBeforeReset = countBeforeReset,
            targetCount = targetCount,
            wishText = wishText,
            resetType = ResetType.MANUAL,
            resetReason = reason
        )
        return saveResetLog(resetLog)
    }
    
    override suspend fun logAutoReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String
    ): ResetLog {
        val resetLog = ResetLog(
            date = DateUtils.getTodayString(),
            resetTime = DateUtils.getCurrentTimestamp(),
            countBeforeReset = countBeforeReset,
            targetCount = targetCount,
            wishText = wishText,
            resetType = ResetType.MIDNIGHT,
            resetReason = "Daily automatic reset"
        )
        return saveResetLog(resetLog)
    }
    
    override suspend fun logEmergencyReset(
        countBeforeReset: Int,
        targetCount: Int,
        wishText: String,
        reason: String
    ): ResetLog {
        val resetLog = ResetLog(
            date = DateUtils.getTodayString(),
            resetTime = DateUtils.getCurrentTimestamp(),
            countBeforeReset = countBeforeReset,
            targetCount = targetCount,
            wishText = wishText,
            resetType = ResetType.EMERGENCY,
            resetReason = reason
        )
        return saveResetLog(resetLog)
    }
    
    override suspend fun deleteResetLog(id: Long): Boolean {
        val log = resetLogDao.getRecentLogs(Int.MAX_VALUE)
            .find { it.id == id }
        return if (log != null) {
            resetLogDao.delete(log)
            true
        } else {
            false
        }
    }
    
    override suspend fun deleteOldResetLogs(beforeDate: String): Int {
        return resetLogDao.deleteOlderThan(beforeDate)
    }
    
    override suspend fun getResetFrequencyAnalysis(): ResetFrequencyAnalysis {
        val allLogs = getRecentResetLogs(Int.MAX_VALUE)
        
        if (allLogs.isEmpty()) {
            return ResetFrequencyAnalysis(
                dailyAverage = 0f,
                weeklyAverage = 0f,
                monthlyAverage = 0f,
                mostFrequentDay = null,
                mostFrequentTime = null,
                resetsByDayOfWeek = emptyMap(),
                resetsByHour = emptyMap()
            )
        }
        
        // Calculate date range
        val dates = allLogs.map { LocalDate.parse(it.date) }
        val firstDate = dates.minOrNull() ?: LocalDate.now()
        val lastDate = dates.maxOrNull() ?: LocalDate.now()
        val totalDays = (lastDate.toEpochDay() - firstDate.toEpochDay() + 1).toInt()
        val totalWeeks = (totalDays + 6) / 7
        val totalMonths = ((lastDate.year - firstDate.year) * 12 + 
                          (lastDate.monthValue - firstDate.monthValue) + 1)
        
        // Calculate averages
        val dailyAverage = allLogs.size.toFloat() / totalDays
        val weeklyAverage = allLogs.size.toFloat() / totalWeeks
        val monthlyAverage = allLogs.size.toFloat() / totalMonths
        
        // Group by day of week
        val resetsByDayOfWeek = allLogs.groupBy {
            val date = LocalDate.parse(it.date)
            date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        }.mapValues { it.value.size }
        
        // Group by hour
        val resetsByHour = allLogs.groupBy {
            val instant = Instant.ofEpochMilli(it.resetTime)
            val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            dateTime.hour
        }.mapValues { it.value.size }
        
        // Find most frequent day and time
        val mostFrequentDay = resetsByDayOfWeek.maxByOrNull { it.value }?.key
        val mostFrequentHour = resetsByHour.maxByOrNull { it.value }?.key
        val mostFrequentTime = mostFrequentHour?.let { 
            String.format("%02d:00-%02d:00", it, (it + 1) % 24)
        }
        
        return ResetFrequencyAnalysis(
            dailyAverage = dailyAverage,
            weeklyAverage = weeklyAverage,
            monthlyAverage = monthlyAverage,
            mostFrequentDay = mostFrequentDay,
            mostFrequentTime = mostFrequentTime,
            resetsByDayOfWeek = resetsByDayOfWeek,
            resetsByHour = resetsByHour
        )
    }
    
    override suspend fun getResetImpactAnalysis(): ResetImpactAnalysis {
        val allLogs = getRecentResetLogs(Int.MAX_VALUE)
        
        if (allLogs.isEmpty()) {
            return ResetImpactAnalysis(
                totalLostCount = 0,
                averageLostCount = 0f,
                maxLostCount = 0,
                minLostCount = 0,
                lostCountByType = emptyMap(),
                mostImpactfulReset = null,
                recoveryRate = 0f
            )
        }
        
        val totalLostCount = allLogs.sumOf { it.countBeforeReset }
        val averageLostCount = totalLostCount.toFloat() / allLogs.size
        val maxLostCount = allLogs.maxOf { it.countBeforeReset }
        val minLostCount = allLogs.filter { it.countBeforeReset > 0 }
            .minOfOrNull { it.countBeforeReset } ?: 0
        
        val lostCountByType = ResetType.values().associateWith { type ->
            allLogs.filter { it.resetType == type }
                .sumOf { it.countBeforeReset }
        }
        
        val mostImpactfulReset = allLogs.maxByOrNull { it.countBeforeReset }
        
        // Calculate recovery rate (simplified - could be enhanced)
        // Recovery rate = average progress after reset / average lost count
        val recoveryRate = if (averageLostCount > 0) {
            // This is a simplified calculation
            // In a real implementation, we'd track actual recovery data
            0.7f // Placeholder value
        } else {
            0f
        }
        
        return ResetImpactAnalysis(
            totalLostCount = totalLostCount,
            averageLostCount = averageLostCount,
            maxLostCount = maxLostCount,
            minLostCount = minLostCount,
            lostCountByType = lostCountByType,
            mostImpactfulReset = mostImpactfulReset,
            recoveryRate = recoveryRate
        )
    }
}