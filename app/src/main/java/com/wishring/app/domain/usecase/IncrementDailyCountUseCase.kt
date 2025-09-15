package com.wishring.app.domain.usecase

import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import javax.inject.Inject

/**
 * Use case for incrementing the daily count
 * Designed for BLE integration where button presses increment the shared daily count
 */
class IncrementDailyCountUseCase @Inject constructor(
    private val wishCountRepository: WishCountRepository
) {
    
    /**
     * Increment the daily count (shared across all wishes)
     * @param amount Amount to increment (default 1)
     * @return Updated WishCount or null if failed
     */
    suspend fun execute(amount: Int = 1): Result<WishCount> {
        return try {
            val updatedWishCount = wishCountRepository.incrementTodayCount(amount)
            Result.success(updatedWishCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}