package com.wishring.app.data.repository

import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.dao.WishDao
import com.wishring.app.data.local.database.entity.WishEntity
import com.wishring.app.data.local.database.entity.WishData
import com.wishring.app.data.model.DailyRecord
import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.model.toEntity
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
class WishRepositoryImpl @Inject constructor(
    private val wishDao: WishDao,
    private val preferencesRepository: PreferencesRepository
) : WishRepository {

    override suspend fun getTodayWish(): WishUiState? {
        val today = DateUtils.getTodayString()
        val existingCount = wishDao.getByDate(today)

        // Return null if no wish exists for today (don't create default)
        return existingCount?.let {
            WishUiState.fromEntity(it)
        }
    }

    override suspend fun getWishCountByDate(date: String): WishUiState? {
        return wishDao.getByDate(date)?.let { WishUiState.fromEntity(it) }
    }

    override fun getAllWishCounts(): Flow<List<WishUiState>> {
        return wishDao.getAllRecords().map { entities ->
            entities.map { WishUiState.fromEntity(it) }
        }
    }

    override suspend fun getWishCountsBetween(
        startDate: String,
        endDate: String
    ): List<WishUiState> {
        return wishDao.getRecordsBetween(startDate, endDate).map {
            WishUiState.fromEntity(it)
        }
    }

    override suspend fun getRecentWishCounts(limit: Int): List<WishUiState> {
        return wishDao.getRecentRecords(limit).first().map { entity ->
            WishUiState.fromEntity(entity)
        }
    }

    override suspend fun getDailyRecords(limit: Int): List<DailyRecord> {
        val wishCounts = wishDao.getRecentRecords(limit).first()
        return wishCounts.map { entity ->
            val wishUiState = WishUiState.fromEntity(entity)
            DailyRecord.fromWishCount(wishUiState)
        }
    }

    /**
     * Seed database with dummy data for testing
     * Creates 30 days of historical wish data
     */
    override suspend fun seedDummyData() {
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

        // Generate 30 days of historical data
        for (daysAgo in 1..30) {
            val date = LocalDate.now().minusDays(daysAgo.toLong())
            val dateString = date.toString()

            // Skip if data already exists for this date
            val existing = wishDao.getByDate(dateString)
            if (existing != null) continue

            // Create random wish data
            val wishText = wishTexts.random()
            val targetCount = listOf(100, 500, 1000, 2000).random()
            val isCompleted = daysAgo % 3 != 0 // 2/3 probability of completion
            val totalCount =
                if (isCompleted) targetCount else ((targetCount * 0.2).toInt()..(targetCount * 0.95).toInt()).random()

            // Create single wish in WishData format
            val wishData = listOf(WishData(text = wishText))

            val entity = WishEntity.createWithWishes(
                wishes = wishData,
                targetCount = targetCount,
                activeIndex = 0,
                date = dateString
            ).copy(
                totalCount = totalCount,
                isCompleted = isCompleted
            )

            wishDao.insert(entity)
        }
    }

    /**
     * Get or create today's wish count
     * This is used when we need to ensure a wish exists (e.g., when saving from input screen)
     */
    suspend fun getOrCreateTodayWishCount(): WishUiState {
        val today = DateUtils.getTodayString()
        val existingCount = wishDao.getByDate(today)

        return if (existingCount != null) {
            WishUiState.fromEntity(existingCount)
        } else {
            // Create a new wish only when explicitly needed
            val defaultWishText = preferencesRepository.getDefaultWishText()
            val defaultTargetCount = preferencesRepository.getDefaultTargetCount()
            val newCount = WishUiState.createDefault(
                date = today,
                wishText = defaultWishText
            )
            wishDao.insert(newCount.toEntity())
            newCount
        }
    }

    override suspend fun getDailyRecord(date: String): DailyRecord? {
        val wishUiState = wishDao.getByDate(date)?.let { WishUiState.fromEntity(it) }
            ?: return null

        return DailyRecord.fromWishCount(wishUiState)
    }

    override suspend fun saveWishCount(wishUiState: WishUiState): WishUiState {
        val id = wishDao.insert(wishUiState.toEntity())
        return wishUiState
    }

    override suspend fun isTodayCompleted(): Boolean {
        val today = DateUtils.getTodayString()
        val entity = wishDao.getByDate(today)
        return entity?.isCompleted == true
    }

    override suspend fun deleteWishCount(date: String): Boolean {
        return wishDao.deleteWishCount(date) > 0
    }

    override suspend fun deleteOldRecords(beforeDate: String): Int {
        return wishDao.deleteOlderThan(beforeDate)
    }

    override fun observeTodayWishCount(): Flow<WishUiState?> {
        val today = DateUtils.getTodayString()
        return wishDao.observeByDate(today).map { entity ->
            entity?.let { WishUiState.fromEntity(it) }
        }
    }

    override fun observeRecentWishCounts(limit: Int): Flow<List<WishUiState>> {
        return wishDao.getRecentRecords(limit).map { entities ->
            entities.map { WishUiState.fromEntity(it) }
        }
    }

    override suspend fun updateTodayWishesAndTarget(
        wishesData: List<WishData>,
        targetCount: Int,
        activeWishIndex: Int
    ): WishUiState {
        val today = DateUtils.getTodayString()
        val existing = wishDao.getByDate(today)

        val updatedEntity = existing?.updateWishes(
            wishesData,
            targetCount,
            activeWishIndex.coerceIn(0, wishesData.size - 1)
        ) ?: WishEntity.createWithWishes(
            wishes = wishesData,
            targetCount = targetCount,
            activeIndex = activeWishIndex.coerceIn(0, wishesData.size - 1),
            date = today
        )

        wishDao.insert(updatedEntity)
        return WishUiState.fromEntity(updatedEntity)
    }

    override suspend fun setActiveWishIndex(index: Int): WishUiState {
        val today = DateUtils.getTodayString()
        val existing = wishDao.getByDate(today)
            ?: throw IllegalStateException("No wish count found for today")

        val wishes = existing.parseWishes()
        val validIndex = index.coerceIn(0, wishes.size - 1)
        val updatedEntity = existing.updateWishes(wishes, validIndex)

        wishDao.insert(updatedEntity)
        return WishUiState.fromEntity(updatedEntity)
    }

    override suspend fun getActiveWishIndex(): Int {
        val today = DateUtils.getTodayString()
        val existing = wishDao.getByDate(today)
        return existing?.activeWishIndex ?: 0
    }

    override suspend fun getTodayWishes(): List<WishData> {
        val today = DateUtils.getTodayString()
        val existing = wishDao.getByDate(today)
        return existing?.parseWishes() ?: emptyList()
    }
}