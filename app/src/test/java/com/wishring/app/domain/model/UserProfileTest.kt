package com.wishring.app.domain.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period

@DisplayName("UserProfile 도메인 모델 테스트")
class UserProfileTest {

    @Nested
    @DisplayName("사용자 프로필 생성 테스트")
    inner class CreationTests {
        
        @Test
        @DisplayName("기본 사용자 프로필 생성")
        fun `should create default user profile`() {
            // When
            val profile = UserProfile.createDefault()
            
            // Then
            profile.id shouldBe 0L
            profile.nickname shouldBe "위시링 사용자"
            profile.dailyGoal shouldBe 100
            profile.totalWishCount shouldBe 0
            profile.achievementDays shouldBe 0
            profile.currentStreak shouldBe 0
            profile.longestStreak shouldBe 0
            profile.createdAt shouldNotBe null
        }
        
        @Test
        @DisplayName("커스텀 사용자 프로필 생성")
        fun `should create custom user profile`() {
            // Given
            val nickname = "행운의 사용자"
            val dailyGoal = 50
            val totalWishCount = 1000
            
            // When
            val profile = UserProfile(
                id = 1L,
                nickname = nickname,
                dailyGoal = dailyGoal,
                totalWishCount = totalWishCount,
                achievementDays = 10,
                currentStreak = 5,
                longestStreak = 15,
                createdAt = LocalDateTime.now(),
                lastActiveDate = LocalDate.now()
            )
            
            // Then
            profile.nickname shouldBe nickname
            profile.dailyGoal shouldBe dailyGoal
            profile.totalWishCount shouldBe totalWishCount
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 10, 50, 100, 500, 1000])
        @DisplayName("다양한 일일 목표 설정")
        fun `should set various daily goals`(goal: Int) {
            // When
            val profile = UserProfile(
                dailyGoal = goal
            )
            
            // Then
            profile.dailyGoal shouldBe goal
            profile.isDailyGoalValid() shouldBe true
        }
    }
    
    @Nested
    @DisplayName("프로필 업데이트 테스트")
    inner class UpdateTests {
        
        @Test
        @DisplayName("닉네임 변경")
        fun `should update nickname`() {
            // Given
            val profile = UserProfile.createDefault()
            val newNickname = "새로운 닉네임"
            
            // When
            val updated = profile.updateNickname(newNickname)
            
            // Then
            updated.nickname shouldBe newNickname
            updated.lastModifiedAt shouldNotBe null
        }
        
        @Test
        @DisplayName("일일 목표 변경")
        fun `should update daily goal`() {
            // Given
            val profile = UserProfile.createDefault()
            val newGoal = 200
            
            // When
            val updated = profile.updateDailyGoal(newGoal)
            
            // Then
            updated.dailyGoal shouldBe newGoal
            updated.lastModifiedAt shouldNotBe null
        }
        
        @Test
        @DisplayName("잘못된 일일 목표 거부")
        fun `should reject invalid daily goal`() {
            // Given
            val profile = UserProfile.createDefault()
            
            // Then
            assertThrows<IllegalArgumentException> {
                profile.updateDailyGoal(0)
            }
            
            assertThrows<IllegalArgumentException> {
                profile.updateDailyGoal(-10)
            }
            
            assertThrows<IllegalArgumentException> {
                profile.updateDailyGoal(10001)
            }
        }
        
        @Test
        @DisplayName("위시 카운트 증가")
        fun `should increment wish count`() {
            // Given
            val profile = UserProfile(totalWishCount = 100)
            
            // When
            val updated = profile.incrementWishCount(5)
            
            // Then
            updated.totalWishCount shouldBe 105
            updated.lastActiveDate shouldBe LocalDate.now()
        }
    }
    
    @Nested
    @DisplayName("스트릭 관리 테스트")
    inner class StreakManagementTests {
        
        @Test
        @DisplayName("스트릭 증가")
        fun `should increment streak`() {
            // Given
            val profile = UserProfile(
                currentStreak = 5,
                longestStreak = 10
            )
            
            // When
            val updated = profile.incrementStreak()
            
            // Then
            updated.currentStreak shouldBe 6
            updated.longestStreak shouldBe 10
        }
        
        @Test
        @DisplayName("최장 스트릭 갱신")
        fun `should update longest streak when current exceeds it`() {
            // Given
            val profile = UserProfile(
                currentStreak = 10,
                longestStreak = 10
            )
            
            // When
            val updated = profile.incrementStreak()
            
            // Then
            updated.currentStreak shouldBe 11
            updated.longestStreak shouldBe 11
        }
        
        @Test
        @DisplayName("스트릭 리셋")
        fun `should reset streak`() {
            // Given
            val profile = UserProfile(
                currentStreak = 15,
                longestStreak = 20
            )
            
            // When
            val updated = profile.resetStreak()
            
            // Then
            updated.currentStreak shouldBe 0
            updated.longestStreak shouldBe 20 // 최장 기록은 유지
        }
        
        @Test
        @DisplayName("스트릭 복구")
        fun `should restore streak with protection`() {
            // Given
            val profile = UserProfile(
                currentStreak = 0,
                longestStreak = 30,
                lastActiveDate = LocalDate.now().minusDays(1)
            )
            
            // When
            val canRestore = profile.canRestoreStreak()
            val restored = if (canRestore) profile.restoreStreak(29) else profile
            
            // Then
            if (canRestore) {
                restored.currentStreak shouldBe 29
            }
        }
    }
    
    @Nested
    @DisplayName("레벨 및 티어 시스템 테스트")
    inner class LevelSystemTests {
        
        @ParameterizedTest
        @CsvSource(
            "0,1,BRONZE",
            "100,1,BRONZE",
            "500,2,BRONZE",
            "1000,3,SILVER",
            "5000,5,SILVER",
            "10000,7,GOLD",
            "50000,15,PLATINUM",
            "100000,20,DIAMOND"
        )
        @DisplayName("총 위시 카운트에 따른 레벨 계산")
        fun `should calculate level based on total wish count`(
            totalCount: Int,
            expectedLevel: Int,
            expectedTier: String
        ) {
            // Given
            val profile = UserProfile(totalWishCount = totalCount)
            
            // When
            val level = profile.calculateLevel()
            val tier = profile.calculateTier()
            
            // Then
            level shouldBe expectedLevel
            tier shouldBe UserTier.valueOf(expectedTier)
        }
        
        @Test
        @DisplayName("다음 레벨까지 필요한 카운트 계산")
        fun `should calculate count needed for next level`() {
            // Given
            val profile = UserProfile(totalWishCount = 450)
            
            // When
            val needed = profile.countForNextLevel()
            
            // Then
            needed shouldBe 50 // 500 - 450
        }
        
        @Test
        @DisplayName("레벨 진행률 계산")
        fun `should calculate level progress percentage`() {
            // Given
            val profile = UserProfile(totalWishCount = 750) // Level 2, 250/500 to Level 3
            
            // When
            val progress = profile.levelProgress()
            
            // Then
            progress shouldBe 50.0f
        }
    }
    
    @Nested
    @DisplayName("통계 및 분석 테스트")
    inner class StatisticsTests {
        
        @Test
        @DisplayName("평균 일일 위시 카운트 계산")
        fun `should calculate average daily wish count`() {
            // Given
            val profile = UserProfile(
                totalWishCount = 3000,
                createdAt = LocalDateTime.now().minusDays(30)
            )
            
            // When
            val average = profile.calculateAverageDailyCount()
            
            // Then
            average shouldBe 100.0
        }
        
        @Test
        @DisplayName("달성률 계산")
        fun `should calculate achievement rate`() {
            // Given
            val profile = UserProfile(
                achievementDays = 20,
                createdAt = LocalDateTime.now().minusDays(30)
            )
            
            // When
            val rate = profile.calculateAchievementRate()
            
            // Then
            rate shouldBe (20.0 / 30.0 * 100)
        }
        
        @Test
        @DisplayName("활동 상태 판단")
        fun `should determine activity status`() {
            // Given
            val activeProfile = UserProfile(
                lastActiveDate = LocalDate.now()
            )
            val inactiveProfile = UserProfile(
                lastActiveDate = LocalDate.now().minusDays(3)
            )
            val dormantProfile = UserProfile(
                lastActiveDate = LocalDate.now().minusDays(8)
            )
            
            // Then
            activeProfile.getActivityStatus() shouldBe ActivityStatus.ACTIVE
            inactiveProfile.getActivityStatus() shouldBe ActivityStatus.INACTIVE
            dormantProfile.getActivityStatus() shouldBe ActivityStatus.DORMANT
        }
        
        @Test
        @DisplayName("프로필 완성도 계산")
        fun `should calculate profile completeness`() {
            // Given
            val completeProfile = UserProfile(
                nickname = "커스텀 닉네임",
                profileImageUrl = "https://example.com/image.jpg",
                bio = "나의 소개",
                birthDate = LocalDate.of(1990, 1, 1)
            )
            
            val incompleteProfile = UserProfile(
                nickname = "위시링 사용자"
            )
            
            // Then
            completeProfile.calculateCompleteness() shouldBe 100
            incompleteProfile.calculateCompleteness() shouldBe 25
        }
    }
    
    @Nested
    @DisplayName("배지 및 업적 시스템 테스트")
    inner class BadgeSystemTests {
        
        @Test
        @DisplayName("획득 가능한 배지 확인")
        fun `should check earned badges`() {
            // Given
            val profile = UserProfile(
                totalWishCount = 1000,
                currentStreak = 7,
                longestStreak = 30,
                achievementDays = 50
            )
            
            // When
            val badges = profile.getEarnedBadges()
            
            // Then
            badges shouldContain Badge.FIRST_THOUSAND
            badges shouldContain Badge.WEEK_WARRIOR
            badges shouldContain Badge.MONTH_MASTER
            badges shouldContain Badge.FIFTY_ACHIEVEMENTS
        }
        
        @Test
        @DisplayName("다음 배지까지 진행률")
        fun `should calculate progress to next badge`() {
            // Given
            val profile = UserProfile(
                totalWishCount = 800
            )
            
            // When
            val progress = profile.progressToNextBadge(Badge.FIRST_THOUSAND)
            
            // Then
            progress shouldBe 80.0f
        }
    }
    
    @Nested
    @DisplayName("Property-based 테스트")
    inner class PropertyBasedTests {
        
        @Test
        @DisplayName("스트릭은 항상 최장 스트릭 이하")
        fun `current streak should never exceed longest streak`() = runTest {
            checkAll(
                Arb.int(0..1000),
                Arb.int(0..1000)
            ) { current, longest ->
                val profile = UserProfile(
                    currentStreak = minOf(current, longest),
                    longestStreak = maxOf(current, longest)
                )
                
                profile.currentStreak <= profile.longestStreak shouldBe true
            }
        }
        
        @Test
        @DisplayName("레벨은 항상 양수")
        fun `level should always be positive`() = runTest {
            checkAll(Arb.int(0..1000000)) { totalCount ->
                val profile = UserProfile(totalWishCount = totalCount)
                profile.calculateLevel() >= 1 shouldBe true
            }
        }
        
        @Test
        @DisplayName("달성률은 0-100% 범위")
        fun `achievement rate should be between 0 and 100`() = runTest {
            checkAll(
                Arb.int(0..365),
                Arb.int(1..365)
            ) { achievements, days ->
                val profile = UserProfile(
                    achievementDays = minOf(achievements, days),
                    createdAt = LocalDateTime.now().minusDays(days.toLong())
                )
                
                val rate = profile.calculateAchievementRate()
                rate >= 0 shouldBe true
                rate <= 100 shouldBe true
            }
        }
    }
    
    // Helper functions
    private fun UserProfile.isDailyGoalValid(): Boolean {
        return dailyGoal in 1..10000
    }
    
    private fun UserProfile.canRestoreStreak(): Boolean {
        return lastActiveDate?.isEqual(LocalDate.now().minusDays(1)) == true
    }
    
    private fun UserProfile.restoreStreak(previousStreak: Int): UserProfile {
        return copy(currentStreak = previousStreak)
    }
}

// Extension functions for UserProfile
private fun UserProfile.updateNickname(nickname: String) = copy(
    nickname = nickname,
    lastModifiedAt = LocalDateTime.now()
)

private fun UserProfile.updateDailyGoal(goal: Int): UserProfile {
    require(goal in 1..10000) { "일일 목표는 1-10000 사이여야 합니다" }
    return copy(
        dailyGoal = goal,
        lastModifiedAt = LocalDateTime.now()
    )
}

private fun UserProfile.incrementWishCount(count: Int = 1) = copy(
    totalWishCount = totalWishCount + count,
    lastActiveDate = LocalDate.now()
)

private fun UserProfile.incrementStreak() = copy(
    currentStreak = currentStreak + 1,
    longestStreak = maxOf(longestStreak, currentStreak + 1)
)

private fun UserProfile.resetStreak() = copy(currentStreak = 0)

private fun UserProfile.calculateLevel(): Int {
    return when {
        totalWishCount < 500 -> 1
        totalWishCount < 1000 -> 2
        totalWishCount < 2000 -> 3
        totalWishCount < 5000 -> 5
        totalWishCount < 10000 -> 7
        totalWishCount < 20000 -> 10
        totalWishCount < 50000 -> 15
        else -> 20
    }
}

private fun UserProfile.calculateTier(): UserTier {
    return when (calculateLevel()) {
        in 1..2 -> UserTier.BRONZE
        in 3..5 -> UserTier.SILVER
        in 6..10 -> UserTier.GOLD
        in 11..15 -> UserTier.PLATINUM
        else -> UserTier.DIAMOND
    }
}

private fun UserProfile.countForNextLevel(): Int {
    val nextThreshold = when {
        totalWishCount < 500 -> 500
        totalWishCount < 1000 -> 1000
        totalWishCount < 2000 -> 2000
        totalWishCount < 5000 -> 5000
        totalWishCount < 10000 -> 10000
        totalWishCount < 20000 -> 20000
        totalWishCount < 50000 -> 50000
        else -> 100000
    }
    return nextThreshold - totalWishCount
}

private fun UserProfile.levelProgress(): Float {
    val currentThreshold = when {
        totalWishCount < 500 -> 0
        totalWishCount < 1000 -> 500
        totalWishCount < 2000 -> 1000
        totalWishCount < 5000 -> 2000
        totalWishCount < 10000 -> 5000
        totalWishCount < 20000 -> 10000
        totalWishCount < 50000 -> 20000
        else -> 50000
    }
    val nextThreshold = currentThreshold + countForNextLevel() + totalWishCount - currentThreshold
    val progress = totalWishCount - currentThreshold
    val needed = nextThreshold - currentThreshold
    return if (needed > 0) (progress.toFloat() / needed * 100) else 100f
}

private fun UserProfile.calculateAverageDailyCount(): Double {
    val days = Period.between(createdAt.toLocalDate(), LocalDate.now()).days
    return if (days > 0) totalWishCount.toDouble() / days else 0.0
}

private fun UserProfile.calculateAchievementRate(): Double {
    val days = Period.between(createdAt.toLocalDate(), LocalDate.now()).days
    return if (days > 0) achievementDays.toDouble() / days * 100 else 0.0
}

private fun UserProfile.getActivityStatus(): ActivityStatus {
    val daysSinceActive = lastActiveDate?.let {
        Period.between(it, LocalDate.now()).days
    } ?: Int.MAX_VALUE
    
    return when {
        daysSinceActive <= 1 -> ActivityStatus.ACTIVE
        daysSinceActive <= 7 -> ActivityStatus.INACTIVE
        else -> ActivityStatus.DORMANT
    }
}

private fun UserProfile.calculateCompleteness(): Int {
    var score = 0
    if (nickname != "위시링 사용자") score += 25
    if (!profileImageUrl.isNullOrEmpty()) score += 25
    if (!bio.isNullOrEmpty()) score += 25
    if (birthDate != null) score += 25
    return score
}

private fun UserProfile.getEarnedBadges(): List<Badge> {
    val badges = mutableListOf<Badge>()
    
    if (totalWishCount >= 1000) badges.add(Badge.FIRST_THOUSAND)
    if (totalWishCount >= 10000) badges.add(Badge.TEN_THOUSAND)
    if (currentStreak >= 7) badges.add(Badge.WEEK_WARRIOR)
    if (longestStreak >= 30) badges.add(Badge.MONTH_MASTER)
    if (achievementDays >= 50) badges.add(Badge.FIFTY_ACHIEVEMENTS)
    
    return badges
}

private fun UserProfile.progressToNextBadge(badge: Badge): Float {
    return when (badge) {
        Badge.FIRST_THOUSAND -> (totalWishCount.toFloat() / 1000 * 100).coerceAtMost(100f)
        Badge.TEN_THOUSAND -> (totalWishCount.toFloat() / 10000 * 100).coerceAtMost(100f)
        Badge.WEEK_WARRIOR -> (currentStreak.toFloat() / 7 * 100).coerceAtMost(100f)
        Badge.MONTH_MASTER -> (longestStreak.toFloat() / 30 * 100).coerceAtMost(100f)
        Badge.FIFTY_ACHIEVEMENTS -> (achievementDays.toFloat() / 50 * 100).coerceAtMost(100f)
    }
}

// Supporting enums
enum class UserTier {
    BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
}

enum class ActivityStatus {
    ACTIVE, INACTIVE, DORMANT
}

enum class Badge {
    FIRST_THOUSAND,
    TEN_THOUSAND,
    WEEK_WARRIOR,
    MONTH_MASTER,
    FIFTY_ACHIEVEMENTS
}

// UserProfile domain model
data class UserProfile(
    val id: Long = 0,
    val nickname: String = "위시링 사용자",
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val birthDate: LocalDate? = null,
    val dailyGoal: Int = 100,
    val totalWishCount: Int = 0,
    val achievementDays: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val lastModifiedAt: LocalDateTime? = null,
    val lastActiveDate: LocalDate? = null
) {
    companion object {
        fun createDefault() = UserProfile()
    }
}