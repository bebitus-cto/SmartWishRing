package com.wishring.app.presentation.wishinput.model

import com.wishring.app.data.local.database.entity.WishData
import java.time.LocalDate
import java.util.UUID

/**
 * Data class representing a single wish item
 * Used for multiple wish registration (max 3 items)
 */
data class WishItem(
    val id: UUID = UUID.randomUUID(),
    val text: String = "",
    val targetCount: Int = 1000,
    val creationDate: LocalDate = LocalDate.now()
) {
    /**
     * Check if this wish is valid for saving
     */
    val isValid: Boolean
        get() = text.isNotBlank() && targetCount > 0
    
    /**
     * Check if this wish was created today
     */
    val isToday: Boolean
        get() = creationDate == LocalDate.now()
    
    /**
     * Get display text for target count
     */
    val targetCountDisplay: String
        get() = "${targetCount}회"
    
    /**
     * Convert to WishData for database storage
     */
    fun toWishData(): WishData {
        return WishData(
            text = text,
            targetCount = targetCount
        )
    }
    
    companion object {
        /**
         * Create empty wish item for input
         */
        fun createEmpty(): WishItem {
            return WishItem()
        }
        
        /**
         * Create wish item with specific text and count
         */
        fun create(text: String, targetCount: Int): WishItem {
            return WishItem(
                text = text,
                targetCount = targetCount
            )
        }
        
        /**
         * Create from WishData
         */
        fun fromWishData(wishData: WishData): WishItem {
            return WishItem(
                text = wishData.text,
                targetCount = wishData.targetCount
            )
        }
    }
}

/**
 * Extension function to convert List<WishItem> to List<WishData>
 */
fun List<WishItem>.toWishDataList(): List<WishData> {
    return this.map { it.toWishData() }
}

/**
 * Extension function to convert List<WishData> to List<WishItem>
 */
fun List<WishData>.toWishItemList(): List<WishItem> {
    return this.map { WishItem.fromWishData(it) }
}