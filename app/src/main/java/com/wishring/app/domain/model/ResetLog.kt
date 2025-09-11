package com.wishring.app.domain.model

import com.wishring.app.core.util.DateUtils
import com.wishring.app.data.local.database.entity.ResetLogEntity
import com.wishring.app.data.local.database.entity.ResetType

/**
 * Domain model for reset log
 * Represents a single reset event with details
 */
data class ResetLog(
    val id: Long = 0,
    val date: String,
    val resetTime: Long,
    val countBeforeReset: Int,
    val targetCount: Int,
    val wishText: String,
    val resetType: ResetType,
    val resetReason: String? = null
) {
    /**
     * Get formatted time for display (HH:mm)
     */
    val displayTime: String
        get() = DateUtils.formatTime(resetTime)
    
    /**
     * Get formatted date for display (yyyy.MM.dd)
     */
    val displayDate: String
        get() = DateUtils.toDisplayFormat(date)
    
    /**
     * Get relative date string (오늘, 어제, N일 전)
     */
    val relativeDateString: String
        get() = DateUtils.getRelativeDateString(date)
    
    /**
     * Get full datetime display (yyyy.MM.dd HH:mm)
     */
    val displayDateTime: String
        get() = "${displayDate} ${displayTime}"
    
    /**
     * Check if this reset happened today
     */
    val isToday: Boolean
        get() = DateUtils.isToday(date)
    
    /**
     * Calculate lost progress percentage
     */
    val lostProgressPercentage: Int
        get() = if (targetCount > 0) {
            ((countBeforeReset.toFloat() / targetCount) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    
    /**
     * Get reset type display text
     */
    val resetTypeText: String
        get() = when (resetType) {
            ResetType.MANUAL -> "수동 초기화"
            ResetType.AUTO -> "자동 초기화"
            ResetType.DAILY -> "일일 초기화"
            ResetType.EMERGENCY -> "긴급 초기화"
            ResetType.MIDNIGHT -> "자정 초기화"
            ResetType.BATTERY -> "배터리 초기화"
            ResetType.ERROR -> "오류 초기화"
            ResetType.UNKNOWN -> "알 수 없음"
        }
    
    /**
     * Get reset type emoji
     */
    val resetTypeEmoji: String
        get() = when (resetType) {
            ResetType.MANUAL -> "👆"
            ResetType.AUTO -> "🔄"
            ResetType.DAILY -> "📅"
            ResetType.EMERGENCY -> "🚨"
            ResetType.MIDNIGHT -> "🌙"
            ResetType.BATTERY -> "🔋"
            ResetType.ERROR -> "⚠️"
            ResetType.UNKNOWN -> "❓"
        }
    
    /**
     * Get impact level based on lost count
     */
    val impactLevel: ImpactLevel
        get() = when {
            countBeforeReset == 0 -> ImpactLevel.NONE
            countBeforeReset < 10 -> ImpactLevel.LOW
            countBeforeReset < 50 -> ImpactLevel.MEDIUM
            countBeforeReset < 100 -> ImpactLevel.HIGH
            else -> ImpactLevel.CRITICAL
        }
    
    /**
     * Get impact level color (for UI)
     */
    val impactColor: String
        get() = when (impactLevel) {
            ImpactLevel.NONE -> "#808080"     // Gray
            ImpactLevel.LOW -> "#4CAF50"      // Green
            ImpactLevel.MEDIUM -> "#FFC107"   // Amber
            ImpactLevel.HIGH -> "#FF9800"     // Orange
            ImpactLevel.CRITICAL -> "#F44336"  // Red
        }
    
    /**
     * Check if this was a significant reset
     */
    val isSignificant: Boolean
        get() = countBeforeReset > 0
    
    /**
     * Get human-readable duration since reset
     */
    val timeSinceReset: String
        get() {
            val now = DateUtils.getCurrentTimestamp()
            val diff = now - resetTime
            
            return when {
                diff < 60_000 -> "방금 전"
                diff < 3_600_000 -> "${diff / 60_000}분 전"
                diff < 86_400_000 -> "${diff / 3_600_000}시간 전"
                else -> relativeDateString
            }
        }
    
    companion object {
        /**
         * Create from Entity
         */
        fun fromEntity(entity: ResetLogEntity): ResetLog {
            return ResetLog(
                id = entity.id,
                date = entity.date,
                resetTime = entity.resetTime,
                countBeforeReset = entity.countBeforeReset,
                targetCount = 0, // Default value as entity doesn't have this field
                wishText = "", // Default value as entity doesn't have this field  
                resetType = ResetType.valueOf(entity.resetType),
                resetReason = entity.notes
            )
        }
        
        /**
         * Create manual reset log
         */
        fun createManualReset(
            wishCount: WishCount,
            reason: String? = null
        ): ResetLog {
            return ResetLog(
                date = wishCount.date,
                resetTime = DateUtils.getCurrentTimestamp(),
                countBeforeReset = wishCount.totalCount,
                targetCount = wishCount.targetCount,
                wishText = wishCount.wishText,
                resetType = ResetType.MANUAL,
                resetReason = reason
            )
        }
        
        /**
         * Create auto reset log (daily reset)
         */
        fun createAutoReset(wishCount: WishCount): ResetLog {
            return ResetLog(
                date = wishCount.date,
                resetTime = DateUtils.getCurrentTimestamp(),
                countBeforeReset = wishCount.totalCount,
                targetCount = wishCount.targetCount,
                wishText = wishCount.wishText,
                resetType = ResetType.DAILY,
                resetReason = "Daily automatic reset"
            )
        }
        
        /**
         * Create emergency reset log
         */
        fun createEmergencyReset(
            wishCount: WishCount,
            reason: String
        ): ResetLog {
            return ResetLog(
                date = wishCount.date,
                resetTime = DateUtils.getCurrentTimestamp(),
                countBeforeReset = wishCount.totalCount,
                targetCount = wishCount.targetCount,
                wishText = wishCount.wishText,
                resetType = ResetType.EMERGENCY,
                resetReason = reason
            )
        }
    }
}

/**
 * Impact level enum for categorizing reset severity
 */
enum class ImpactLevel {
    NONE,      // No count lost
    LOW,       // < 10 count lost
    MEDIUM,    // 10-49 count lost
    HIGH,      // 50-99 count lost
    CRITICAL   // >= 100 count lost
}

/**
 * Extension function to convert Domain Model to Entity
 */
fun ResetLog.toEntity(): ResetLogEntity {
    return ResetLogEntity(
        id = id,
        date = date,
        resetTime = resetTime,
        countBeforeReset = countBeforeReset,
        resetType = resetType.name,
        notes = resetReason
    )
}

/**
 * Extension function to get summary statistics from list of reset logs
 */
fun List<ResetLog>.getSummaryStatistics(): ResetStatistics {
    return ResetStatistics(
        totalResets = size,
        totalLostCount = sumOf { it.countBeforeReset },
        manualResets = count { it.resetType == ResetType.MANUAL },
        autoResets = count { it.resetType == ResetType.AUTO },
        dailyResets = count { it.resetType == ResetType.DAILY },
        emergencyResets = count { it.resetType == ResetType.EMERGENCY },
        averageLostCount = if (isNotEmpty()) sumOf { it.countBeforeReset } / size else 0
    )
}

/**
 * Data class for reset statistics
 */
data class ResetStatistics(
    val totalResets: Int,
    val totalLostCount: Int,
    val manualResets: Int,
    val autoResets: Int,
    val dailyResets: Int,
    val emergencyResets: Int,
    val averageLostCount: Int
)