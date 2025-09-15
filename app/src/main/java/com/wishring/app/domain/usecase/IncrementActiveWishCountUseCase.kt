package com.wishring.app.domain.usecase

import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import javax.inject.Inject

/**
 * Use case for incrementing the count of the currently active wish
 * Designed for BLE integration where button presses should only affect the active wish
 */
class IncrementActiveWishCountUseCase @Inject constructor(
    private val wishCountRepository: WishCountRepository
) {
    
    /**
     * Increment the count of the currently active wish
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
    
    /**
     * Switch to a different active wish and optionally increment
     * @param wishIndex Index of wish to make active (0, 1, or 2)
     * @param incrementAfterSwitch Whether to increment after switching
     * @return Updated WishCount or null if failed
     */
    suspend fun switchActiveWish(
        wishIndex: Int,
        incrementAfterSwitch: Boolean = false
    ): Result<WishCount> {
        return try {
            // First switch the active wish
            val updatedWishCount = wishCountRepository.setActiveWishIndex(wishIndex)
            
            // Optionally increment the newly active wish
            if (incrementAfterSwitch) {
                val incrementedWishCount = wishCountRepository.incrementTodayCount(1)
                Result.success(incrementedWishCount)
            } else {
                Result.success(updatedWishCount)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current active wish index
     * @return Active wish index or null if failed
     */
    suspend fun getActiveWishIndex(): Result<Int> {
        return try {
            val activeIndex = wishCountRepository.getActiveWishIndex()
            Result.success(activeIndex)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}