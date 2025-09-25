package com.wishring.app.data.repository

import com.wishring.app.data.local.database.entity.WishData
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.data.model.WishUiState
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for WishCount domain operations
 * Defines contract between domain and data layers
 */
interface WishRepository {

    /**
     * Get today's wish count
     * Creates new record if doesn't exist
     */
    suspend fun getTodayWish(): WishUiState?

    /**
     * Get wish count for specific date
     * @param date Date in yyyy-MM-dd format
     * @return WishCount or null if not found
     */
    suspend fun getWishCountByDate(date: String): WishUiState?

    /**
     * Get all wish counts as Flow
     * @return Flow of all wish counts ordered by date desc
     */
    fun getAllWishCounts(): Flow<List<WishUiState>>

    /**
     * Get wish counts for date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of wish counts
     */
    suspend fun getWishCountsBetween(startDate: String, endDate: String): List<WishUiState>

    /**
     * Seed database with dummy data for testing
     */
    suspend fun seedDummyData()

    /**
     * Get daily records with reset information
     * @param limit Number of records to fetch
     * @return List of daily records
     */
    suspend fun getWishDays(limit: Int = 30): List<WishDayUiState>

    /**
     * Get daily record for specific date
     * @param date Date in yyyy-MM-dd format
     * @return WishDayUiState with reset info
     */
    suspend fun getWishDay(date: String): WishDayUiState?

    /**
     * Insert or update wish count
     * @param wishUiState WishCount to save
     * @return Saved WishCount with ID
     */
    suspend fun saveWishCount(wishUiState: WishUiState): WishUiState

    /**
     * Set active wish index
     * @param index Index of wish to make active (0, 1, or 2)
     * @return Updated WishCount
     */
    suspend fun setActiveWishIndex(index: Int): WishUiState

    /**
     * Get current active wish index
     * @return Active wish index
     */
    suspend fun getActiveWishIndex(): Int

    /**
     * Get today's wishes as list
     * @return List of WishData for today
     */
    suspend fun getTodayWishes(): List<WishData>

    /**
     * Check if today's target is completed
     * @return True if completed
     */
    suspend fun isTodayCompleted(): Boolean

    /**
     * Delete wish count
     * @param date Date of record to delete
     * @return True if deleted
     */
    suspend fun deleteWishCount(date: String): Boolean

    /**
     * Delete old records
     * @param beforeDate Delete records before this date
     * @return Number of deleted records
     */
    suspend fun deleteOldRecords(beforeDate: String): Int

    /**
     * Observe today's wish count
     * @return Flow of today's WishCount
     */
    fun observeTodayWishCount(): Flow<WishUiState?>

    /**
     * Observe recent wish counts
     * @param limit Number of records
     * @return Flow of recent wish counts
     */
    fun observeRecentWishCounts(limit: Int = 7): Flow<List<WishUiState>>
    suspend fun updateTodayWishesAndTarget(
        wishesData: List<WishData>,
        targetCount: Int,
        activeWishIndex: Int
    ): WishUiState

    suspend fun getRecentWishCounts(limit: Int): List<WishUiState>

    /**
     * Get wish history with pagination support
     * @param page Page number (0-based)
     * @param pageSize Number of items per page
     * @return Pair of wish history and page info
     */
    suspend fun getWishHistoryPaginated(page: Int, pageSize: Int = 100): Pair<List<WishDayUiState>, com.wishring.app.presentation.home.PageInfo>
}