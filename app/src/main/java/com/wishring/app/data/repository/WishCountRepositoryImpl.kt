package com.wishring.app.data.repository

import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.dao.ResetLogDao
import com.wishring.app.data.local.database.dao.WishCountDao
import com.wishring.app.data.local.database.entity.ResetType
import com.wishring.app.domain.model.DailyRecord
import com.wishring.app.domain.model.ResetLog
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.model.toEntity
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.StreakInfo
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.WishCountStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of WishCountRepository
 * Manages WishCount data operations using Room DAO directly
 */
@Singleton
class WishCountRepositoryImpl @Inject constructor(
    private val wishCountDao: WishCountDao,
    private val resetLogDao: ResetLogDao,
    private val preferencesRepository: PreferencesRepository
) : WishCountRepository {
    
    override suspend fun getTodayWishCount(): WishCount {
        val today = DateUtils.getTodayString()
        val existingCount = wishCountDao.getByDate(today)
        
        return if (existingCount != null) {
            WishCount.fromEntity(existingCount)
        } else {
            val defaultWishText = preferencesRepository.getDefaultWishText()
            val defaultTargetCount = preferencesRepository.getDefaultTargetCount()
            val newCount = WishCount.createDefault(
                date = today,
                wishText = defaultWishText,
                targetCount = defaultTargetCount
            )
            wishCountDao.insert(newCount.toEntity())
            newCount
        }
    }
    
    override suspend fun getWishCountByDate(date: String): WishCount? {
        return wishCountDao.getByDate(date)?.let { WishCount.fromEntity(it) }
    }
    
    override fun getAllWishCounts(): Flow<List<WishCount>> {
        return wishCountDao.getAllRecords().map { entities ->
            entities.map { WishCount.fromEntity(it) }
        }
    }
    
    override suspend fun getWishCountsBetween(startDate: String, endDate: String): List<WishCount> {
        return wishCountDao.getRecordsBetween(startDate, endDate).map {
            WishCount.fromEntity(it)
        }
    }
    
    override suspend fun getRecentWishCounts(limit: Int): List<WishCount> {
        return wishCountDao.getRecentRecords(limit).first().map { entity ->
            WishCount.fromEntity(entity)
        }
    }
    
    override suspend fun getDailyRecords(limit: Int): List<DailyRecord> {
        // TODO: 실제 데이터베이스 구현 시 아래 주석 해제하고 더미 데이터 제거
        /*
        val wishCounts = wishCountDao.getRecentRecords(limit).first()
        return wishCounts.map { entity ->
            val wishCount = WishCount.fromEntity(entity)
            val resetLogs = resetLogDao.getLogsByDate(entity.date)
            val resetCount = resetLogs.size
            val lostCount = resetLogs.sumOf { it.countBeforeReset }
            
            DailyRecord.fromWishCount(wishCount, resetCount, lostCount)
        }
        */
        
        // 테스트용 더미 데이터
        val wishTexts = listOf(
            "나는 어제보다 더 나은 내가 되고 있다.",
            "매일 조금씩, 나는 내 가능성을 확장하고 있다.",
            "나는 매일 배우고, 이해하고, 발전하고 있다.",
            "변화는 두렵지 않다, 나는 변화 속에서 자란다.",
            "나는 매일 감사한 마음으로 살아간다.",
            "오늘 하루도 최선을 다해 살아가고 있다.",
            "나는 끊임없이 성장하고 발전하는 사람이다.",
            "매 순간 긍정적인 에너지를 발산하고 있다.",
            "나는 내 꿈을 향해 한 걸음씩 나아가고 있다.",
            "오늘도 새로운 것을 배우며 성장한다.",
            "나는 도전을 두려워하지 않는 용기있는 사람이다.",
            "매일 건강하고 행복한 하루를 만들어간다.",
            "나는 주변 사람들에게 좋은 영향을 주는 사람이다.",
            "작은 일상에서도 기쁨을 찾아 살아간다.",
            "나는 끝까지 포기하지 않는 끈기있는 사람이다."
        )
        
        return (1..limit.coerceAtMost(3)).map { index ->
            val daysAgo = index - 1
            val date = java.time.LocalDate.now().minusDays(daysAgo.toLong())
            val wishText = wishTexts[index % wishTexts.size]
            val isCompleted = index % 3 != 0 // 2/3 확률로 완료
            val totalCount = if (isCompleted) 1000 else (200..950).random()
            
            DailyRecord(
                date = date,
                totalCount = totalCount,
                wishText = wishText,
                targetCount = 1000,
                isCompleted = isCompleted
            )
        }
    }
    
    override suspend fun getDailyRecord(date: String): DailyRecord? {
        val wishCount = wishCountDao.getByDate(date)?.let { WishCount.fromEntity(it) }
            ?: return null
        
        val resetLogs = resetLogDao.getLogsByDate(date)
        val resetCount = resetLogs.size
        val lostCount = resetLogs.sumOf { it.countBeforeReset }
        
        return DailyRecord.fromWishCount(wishCount, resetCount, lostCount)
    }
    
    override suspend fun getStatistics(startDate: String, endDate: String): WishCountStatistics {
        val wishCounts = wishCountDao.getRecordsBetween(startDate, endDate)
            .map { WishCount.fromEntity(it) }
        
        val totalDays = wishCounts.size
        val completedDays = wishCounts.count { it.isCompleted }
        val totalCount = wishCounts.sumOf { it.totalCount }
        val totalTarget = wishCounts.sumOf { it.targetCount }
        val averageCount = if (totalDays > 0) totalCount.toFloat() / totalDays else 0f
        val averageTarget = if (totalDays > 0) totalTarget.toFloat() / totalDays else 0f
        val completionRate = if (totalDays > 0) completedDays.toFloat() / totalDays else 0f
        val bestDay = wishCounts.maxByOrNull { it.totalCount }
        val worstDay = wishCounts.filter { it.totalCount > 0 }.minByOrNull { it.totalCount }
        
        return WishCountStatistics(
            totalDays = totalDays,
            completedDays = completedDays,
            totalCount = totalCount,
            totalTarget = totalTarget,
            averageCount = averageCount,
            averageTarget = averageTarget,
            completionRate = completionRate,
            bestDay = bestDay,
            worstDay = worstDay
        )
    }
    
    override suspend fun saveWishCount(wishCount: WishCount): WishCount {
        val id = wishCountDao.insert(wishCount.toEntity())
        return wishCount
    }
    
    override suspend fun incrementTodayCount(amount: Int): WishCount {
        val today = getTodayWishCount()
        val updated = today.incrementCount(amount)
        wishCountDao.update(updated.toEntity())
        return updated
    }
    
    override suspend fun updateTodayWishAndTarget(
        wishText: String?,
        targetCount: Int?
    ): WishCount {
        val today = getTodayWishCount()
        val updated = today.updateWishAndTarget(wishText, targetCount)
        wishCountDao.update(updated.toEntity())
        return updated
    }
    
    override suspend fun resetTodayCount(reason: String?): WishCount {
        val today = getTodayWishCount()
        
        // Log the reset
        val resetLog = ResetLog.createManualReset(today, reason)
        resetLogDao.insert(resetLog.toEntity())
        
        // Reset the count
        val reset = today.copy(
            totalCount = 0,
            isCompleted = false,
            updatedAt = DateUtils.getCurrentTimestamp()
        )
        wishCountDao.update(reset.toEntity())
        
        return reset
    }
    
    override suspend fun isTodayCompleted(): Boolean {
        val today = DateUtils.getTodayString()
        val entity = wishCountDao.getByDate(today)
        return entity?.isCompleted == true
    }
    
    override suspend fun getAchievementRate(startDate: String, endDate: String): Float {
        val stats = getStatistics(startDate, endDate)
        return stats.completionRate
    }
    
    override suspend fun getStreakInfo(): StreakInfo {
        val allCounts = wishCountDao.getAllRecordsSync()
            .sortedByDescending { it.date }
        
        if (allCounts.isEmpty()) {
            return StreakInfo(
                currentStreak = 0,
                bestStreak = 0,
                lastActiveDate = null,
                streakStartDate = null,
                isActiveToday = false
            )
        }
        
        val today = DateUtils.getTodayString()
        val isActiveToday = allCounts.any { it.date == today && it.totalCount > 0 }
        
        // Calculate current streak
        var currentStreak = 0
        var currentDate = LocalDate.now()
        var streakStartDate: String? = null
        
        for (i in 0 until 365) { // Check up to 1 year
            val dateStr = currentDate.toString()
            val dayCount = allCounts.find { it.date == dateStr }
            
            if (dayCount != null && dayCount.totalCount > 0) {
                currentStreak++
                streakStartDate = dateStr
                currentDate = currentDate.minusDays(1)
            } else if (i == 0 && !isActiveToday) {
                // Today is not active, check from yesterday
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        
        // Calculate best streak
        var bestStreak = currentStreak
        var tempStreak = 0
        val sortedDates = allCounts.filter { it.totalCount > 0 }
            .map { LocalDate.parse(it.date) }
            .sorted()
        
        if (sortedDates.isNotEmpty()) {
            tempStreak = 1
            for (i in 1 until sortedDates.size) {
                val dayDiff = sortedDates[i].toEpochDay() - sortedDates[i - 1].toEpochDay()
                if (dayDiff == 1L) {
                    tempStreak++
                    bestStreak = maxOf(bestStreak, tempStreak)
                } else {
                    tempStreak = 1
                }
            }
        }
        
        val lastActiveDate = allCounts.firstOrNull { it.totalCount > 0 }?.date
        
        return StreakInfo(
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            lastActiveDate = lastActiveDate,
            streakStartDate = streakStartDate,
            isActiveToday = isActiveToday
        )
    }
    
    override suspend fun deleteWishCount(date: String): Boolean {
        return wishCountDao.deleteWishCount(date) > 0
    }
    
    override suspend fun deleteOldRecords(beforeDate: String): Int {
        return wishCountDao.deleteOlderThan(beforeDate)
    }
    
    override fun observeTodayWishCount(): Flow<WishCount?> {
        val today = DateUtils.getTodayString()
        return wishCountDao.observeByDate(today).map { entity ->
            entity?.let { WishCount.fromEntity(it) }
        }
    }
    
    override fun observeRecentWishCounts(limit: Int): Flow<List<WishCount>> {
        return wishCountDao.getRecentRecords(limit).map { entities ->
            entities.map { WishCount.fromEntity(it) }
        }
    }
}