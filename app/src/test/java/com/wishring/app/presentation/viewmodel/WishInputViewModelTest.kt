package com.wishring.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.presentation.wishinput.WishInputViewModel
import com.wishring.app.presentation.wishinput.WishInputEvent
import com.wishring.app.presentation.wishinput.WishInputViewState
import com.wishring.app.presentation.wishinput.WishInputEffect
import com.wishring.app.presentation.wishinput.ValidationField
import com.wishring.app.presentation.wishinput.model.WishItem
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.string.shouldContain
import app.cash.turbine.test
import java.time.LocalDate
import java.util.UUID

@ExperimentalCoroutinesApi
@DisplayName("WishInputViewModel 멀티위시 테스트")
class WishInputViewModelTest {

    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    
    private lateinit var viewModel: WishInputViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Setup default mocks
        every { savedStateHandle.get<Any>(any()) } returns null
        every { savedStateHandle.set(any(), any<Any>()) } just Runs
        coEvery { preferencesRepository.getDefaultWishText() } returns ""
        coEvery { preferencesRepository.getDefaultTargetCount() } returns 1000
        coEvery { wishCountRepository.getDailyRecord(any()) } returns null
        coEvery { wishCountRepository.updateTodayWishAndTarget(any(), any()) } just Runs
        coEvery { wishCountRepository.getWishCountByDate(any()) } returns null
        coEvery { wishCountRepository.saveWishCount(any()) } just Runs
        
        viewModel = WishInputViewModel(
            wishCountRepository = wishCountRepository,
            preferencesRepository = preferencesRepository,
            savedStateHandle = savedStateHandle
        )
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("다중 위시 관리 테스트")
    inner class MultipleWishManagementTests {
        
        @Test
        @DisplayName("초기 상태는 하나의 빈 위시아이템을 가져야 함")
        fun `should start with one empty wish item`() = runTest {
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 1
                state.wishes.first().text shouldBe ""
                state.wishes.first().targetCount shouldBe 1000
            }
        }
        
        @Test
        @DisplayName("위시 추가 기능")
        fun `should add new wish item`() = runTest {
            // When
            viewModel.onEvent(WishInputEvent.AddWish())
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 2
                state.canAddMoreWishes shouldBe true
            }
        }
        
        @Test
        @DisplayName("최대 3개까지만 위시 추가 가능")
        fun `should limit wishes to maximum of 3`() = runTest {
            // Given - Add 2 more wishes to reach limit
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.AddWish())
            advanceUntilIdle()
            
            // When - Try to add 4th wish
            viewModel.onEvent(WishInputEvent.AddWish())
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 3
                state.canAddMoreWishes shouldBe false
            }
        }
        
        @Test
        @DisplayName("특정 위치에 위시 추가")
        fun `should add wish at specific position`() = runTest {
            // Given
            val firstWishId = viewModel.uiState.value.wishes.first().id
            viewModel.onEvent(WishInputEvent.UpdateWishText(firstWishId, "첫 번째 위시"))
            advanceUntilIdle()
            
            // When - Insert at position 0
            viewModel.onEvent(WishInputEvent.AddWish(position = 0))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 2
                state.wishes[0].text shouldBe ""  // New empty wish
                state.wishes[1].text shouldBe "첫 번째 위시"  // Original wish moved
            }
        }
        
        @Test
        @DisplayName("위시 삭제 기능")
        fun `should remove wish item`() = runTest {
            // Given - Add second wish
            viewModel.onEvent(WishInputEvent.AddWish())
            advanceUntilIdle()
            
            val wishId = viewModel.uiState.value.wishes.last().id
            
            // When
            viewModel.onEvent(WishInputEvent.RemoveWish(wishId))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 1
                state.wishes.none { it.id == wishId } shouldBe true
            }
        }
        
        @Test
        @DisplayName("최소 1개는 유지해야 함")
        fun `should maintain minimum of 1 wish`() = runTest {
            // Given - Only one wish exists
            val wishId = viewModel.uiState.value.wishes.first().id
            
            // When - Try to remove the last wish
            viewModel.onEvent(WishInputEvent.RemoveWish(wishId))
            advanceUntilIdle()
            
            // Then - Should still have one wish
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 1
                state.canRemoveWishes shouldBe false
            }
        }
    }
    
    @Nested
    @DisplayName("위시 텍스트 입력 테스트")
    inner class WishTextInputTests {
        
        @Test
        @DisplayName("위시 텍스트 업데이트")
        fun `should update wish text by ID`() = runTest {
            // Given
            val wishId = viewModel.uiState.value.wishes.first().id
            val newText = "매일 성장하는 나"
            
            // When
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishId, newText))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                val updatedWish = state.wishes.find { it.id == wishId }
                updatedWish?.text shouldBe newText
            }
        }
        
        @Test
        @DisplayName("위시 텍스트 길이 제한")
        fun `should limit wish text length`() = runTest {
            // Given
            val wishId = viewModel.uiState.value.wishes.first().id
            val longText = "a".repeat(201) // Exceeds max length
            
            // When
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishId, longText))
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowValidationError>()
                val validationEffect = effect as WishInputEffect.ShowValidationError
                validationEffect.field shouldBe ValidationField.WISH_TEXT
                validationEffect.message shouldContain "최대"
            }
        }
        
        @Test
        @DisplayName("위시 목표 횟수 업데이트")
        fun `should update wish target count by ID`() = runTest {
            // Given
            val wishId = viewModel.uiState.value.wishes.first().id
            val newCount = 2000
            
            // When
            viewModel.onEvent(WishInputEvent.UpdateWishCount(wishId, newCount))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                val updatedWish = state.wishes.find { it.id == wishId }
                updatedWish?.targetCount shouldBe newCount
            }
        }
        
        @Test
        @DisplayName("목표 횟수 범위 검증")
        fun `should validate target count range`() = runTest {
            // Given
            val wishId = viewModel.uiState.value.wishes.first().id
            
            // When - Try to set count outside valid range
            viewModel.onEvent(WishInputEvent.UpdateWishCount(wishId, 50)) // Below minimum
            advanceUntilIdle()
            
            // Then - Should not update
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                val wish = state.wishes.find { it.id == wishId }
                wish?.targetCount shouldBe 1000 // Should remain unchanged
            }
        }
    }
    
    @Nested
    @DisplayName("위시 제안 기능 테스트")
    inner class WishSuggestionTests {
        
        @Test
        @DisplayName("제안된 위시 선택")
        fun `should select suggested wish`() = runTest {
            // Given
            val suggestedWish = "건강한 하루 보내기"
            
            // When
            viewModel.onEvent(WishInputEvent.SelectSuggestedWish(suggestedWish))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.any { it.text == suggestedWish } shouldBe true
                state.showSuggestions shouldBe false
            }
            
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.VibrateForFeedback>()
            }
        }
        
        @Test
        @DisplayName("빈 위시에 제안 적용")
        fun `should apply suggestion to first empty wish`() = runTest {
            // Given - Multiple wishes with first one empty
            viewModel.onEvent(WishInputEvent.AddWish())
            val wishes = viewModel.uiState.value.wishes
            val secondWishId = wishes[1].id
            viewModel.onEvent(WishInputEvent.UpdateWishText(secondWishId, "기존 위시"))
            advanceUntilIdle()
            
            val suggestedWish = "새로운 제안"
            
            // When
            viewModel.onEvent(WishInputEvent.SelectSuggestedWish(suggestedWish))
            advanceUntilIdle()
            
            // Then - Should apply to first empty wish
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes[0].text shouldBe suggestedWish
                state.wishes[1].text shouldBe "기존 위시"
            }
        }
        
        @Test
        @DisplayName("위시 제안 토글")
        fun `should toggle suggestions visibility`() = runTest {
            // When
            viewModel.onEvent(WishInputEvent.ToggleSuggestions)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.showSuggestions shouldBe true
            }
            
            // When - Toggle again
            viewModel.onEvent(WishInputEvent.ToggleSuggestions)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.showSuggestions shouldBe false
            }
        }
    }
    
    @Nested
    @DisplayName("위시 저장 및 검증 테스트")
    inner class WishSaveValidationTests {
        
        @Test
        @DisplayName("유효한 위시 저장 성공")
        fun `should save valid wishes successfully`() = runTest {
            // Given
            val wishId = viewModel.uiState.value.wishes.first().id
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishId, "매일 성장하는 나"))
            viewModel.onEvent(WishInputEvent.UpdateWishCount(wishId, 2000))
            advanceUntilIdle()
            
            // When
            viewModel.onEvent(WishInputEvent.SaveWish)
            advanceUntilIdle()
            
            // Then
            coVerify { 
                wishCountRepository.updateTodayWishAndTarget("매일 성장하는 나", 2000)
            }
            
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowToast>()
            }
        }
        
        @Test
        @DisplayName("빈 위시 저장 시 검증 오류")
        fun `should show validation error for empty wishes`() = runTest {
            // When - Try to save without any wish text
            viewModel.onEvent(WishInputEvent.SaveWish)
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowValidationError>()
                val validationEffect = effect as WishInputEffect.ShowValidationError
                validationEffect.field shouldBe ValidationField.WISH_TEXT
                validationEffect.message shouldContain "최소 하나"
            }
        }
        
        @Test
        @DisplayName("다중 위시 저장 - 첫 번째 유효한 위시만 저장")
        fun `should save only first valid wish from multiple wishes`() = runTest {
            // Given - Add multiple wishes
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.AddWish())
            
            val wishes = viewModel.uiState.value.wishes
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[0].id, "")) // Empty
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[1].id, "첫 번째 유효한 위시"))
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[2].id, "두 번째 유효한 위시"))
            advanceUntilIdle()
            
            // When
            viewModel.onEvent(WishInputEvent.SaveWish)
            advanceUntilIdle()
            
            // Then - Should save first valid wish
            coVerify { 
                wishCountRepository.updateTodayWishAndTarget("첫 번째 유효한 위시", 1000)
            }
        }
        
        @Test
        @DisplayName("12시 넘김 엣지케이스 처리")
        fun `should handle midnight crossing edge case`() = runTest {
            // Given - Mock different creation date
            val wishId = viewModel.uiState.value.wishes.first().id
            val yesterday = LocalDate.now().minusDays(1)
            
            // Create wish with yesterday's date (simulate midnight crossing)
            val yesterdayWish = WishItem.create("어제의 위시", 1000)
                .copy(creationDate = yesterday)
            
            viewModel.uiState.value = viewModel.uiState.value.copy(
                wishes = listOf(yesterdayWish)
            )
            
            // When
            viewModel.onEvent(WishInputEvent.SaveWish)
            advanceUntilIdle()
            
            // Then - Should show date change message
            viewModel.effect.test {
                val effects = mutableListOf<WishInputEffect>()
                repeat(10) { // Collect multiple effects
                    try {
                        effects.add(expectMostRecentItem())
                    } catch (e: Exception) {
                        break
                    }
                }
                
                val toastEffects = effects.filterIsInstance<WishInputEffect.ShowToast>()
                toastEffects.any { it.message.contains("날짜가 변경") } shouldBe true
            }
        }
    }
    
    @Nested
    @DisplayName("위시 삭제 및 초기화 테스트")
    inner class WishDeletionResetTests {
        
        @Test
        @DisplayName("기존 위시 삭제 확인 다이얼로그 표시")
        fun `should show delete confirmation dialog`() = runTest {
            // When
            viewModel.onEvent(WishInputEvent.ShowDeleteConfirmation)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.showDeleteConfirmation shouldBe true
            }
        }
        
        @Test
        @DisplayName("위시 삭제 확인 실행")
        fun `should execute delete confirmation`() = runTest {
            // Given
            val today = LocalDate.now().toString()
            val existingRecord = WishCount(
                id = 1,
                date = today,
                totalCount = 100,
                wishText = "기존 위시",
                targetCount = 1000
            )
            coEvery { wishCountRepository.getWishCountByDate(today) } returns existingRecord
            
            // When
            viewModel.onEvent(WishInputEvent.ConfirmDelete)
            advanceUntilIdle()
            
            // Then
            coVerify { 
                wishCountRepository.saveWishCount(
                    existingRecord.copy(totalCount = 0, wishText = "")
                )
            }
            
            viewModel.effect.test {
                val effects = mutableListOf<WishInputEffect>()
                repeat(5) {
                    try {
                        effects.add(expectMostRecentItem())
                    } catch (e: Exception) {
                        break
                    }
                }
                
                effects.any { it is WishInputEffect.ShowToast } shouldBe true
                effects.any { it is WishInputEffect.NavigateBack } shouldBe true
            }
        }
        
        @Test
        @DisplayName("위시 텍스트 초기화")
        fun `should clear all wish texts`() = runTest {
            // Given - Add and fill wishes
            val firstWishId = viewModel.uiState.value.wishes.first().id
            viewModel.onEvent(WishInputEvent.UpdateWishText(firstWishId, "첫 번째 위시"))
            viewModel.onEvent(WishInputEvent.AddWish())
            
            val secondWishId = viewModel.uiState.value.wishes.last().id
            viewModel.onEvent(WishInputEvent.UpdateWishText(secondWishId, "두 번째 위시"))
            advanceUntilIdle()
            
            // When
            viewModel.onEvent(WishInputEvent.ClearWishText)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.all { it.text.isEmpty() } shouldBe true
            }
        }
        
        @Test
        @DisplayName("기본값으로 초기화")
        fun `should reset to defaults`() = runTest {
            // Given - Modify state
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.UpdateWishText(viewModel.uiState.value.wishes.first().id, "테스트"))
            advanceUntilIdle()
            
            // When
            viewModel.onEvent(WishInputEvent.ResetToDefaults)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 1
                state.wishes.first().text shouldBe ""
                state.isEditMode shouldBe false
                state.existingRecord shouldBe false
            }
        }
    }
    
    @Nested
    @DisplayName("기존 레코드 로드 테스트")
    inner class ExistingRecordLoadTests {
        
        @Test
        @DisplayName("초기화 시 오늘 기록 확인 및 로드")
        fun `should check and load today's record on init`() = runTest {
            // Given
            val today = LocalDate.now().toString()
            val existingRecord = WishCount(
                id = 1,
                date = today,
                totalCount = 50,
                wishText = "기존 위시",
                targetCount = 1500
            )
            coEvery { wishCountRepository.getDailyRecord(today) } returns existingRecord
            
            // When
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.size shouldBe 1
                state.wishes.first().text shouldBe "기존 위시"
                state.wishes.first().targetCount shouldBe 1500
                state.isEditMode shouldBe true
                state.existingRecord shouldBe true
            }
        }
        
        @Test
        @DisplayName("특정 날짜 레코드 로드")
        fun `should load existing record by date`() = runTest {
            // Given
            val targetDate = "2024-01-15"
            val existingRecord = WishCount(
                id = 1,
                date = targetDate,
                totalCount = 75,
                wishText = "과거 위시",
                targetCount = 2000
            )
            coEvery { wishCountRepository.getDailyRecord(targetDate) } returns existingRecord
            
            // When
            viewModel.onEvent(WishInputEvent.LoadExistingRecord(targetDate))
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.wishes.first().text shouldBe "과거 위시"
                state.wishes.first().targetCount shouldBe 2000
                state.isEditMode shouldBe true
                state.existingRecord shouldBe true
                state.date shouldBe targetDate
            }
        }
        
        @Test
        @DisplayName("빈 레코드는 로드하지 않음")
        fun `should not load empty records`() = runTest {
            // Given
            val today = LocalDate.now().toString()
            val emptyRecord = WishCount(
                id = 1,
                date = today,
                totalCount = 0,
                wishText = "", // Empty text
                targetCount = 1000
            )
            coEvery { wishCountRepository.getDailyRecord(today) } returns emptyRecord
            
            // When
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                savedStateHandle = savedStateHandle
            )
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.isEditMode shouldBe false
                state.existingRecord shouldBe false
                state.wishes.first().text shouldBe ""
            }
        }
    }
    
    @Nested
    @DisplayName("상태 계산 속성 테스트")
    inner class StateComputedPropertiesTests {
        
        @Test
        @DisplayName("저장 가능 상태 계산")
        fun `should compute save enabled state correctly`() = runTest {
            // Initially should be disabled (empty wishes)
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.isSaveEnabled shouldBe false
            }
            
            // Given - Add valid wish
            val wishId = viewModel.uiState.value.wishes.first().id
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishId, "유효한 위시"))
            advanceUntilIdle()
            
            // Then - Should be enabled
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.isSaveEnabled shouldBe true
            }
        }
        
        @Test
        @DisplayName("유효한 위시 개수 계산")
        fun `should count valid wishes correctly`() = runTest {
            // Given - Add multiple wishes with different validity
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.AddWish())
            
            val wishes = viewModel.uiState.value.wishes
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[0].id, "")) // Invalid (empty)
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[1].id, "유효한 위시 1")) // Valid
            viewModel.onEvent(WishInputEvent.UpdateWishText(wishes[2].id, "유효한 위시 2")) // Valid
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.validWishCount shouldBe 2
            }
        }
        
        @Test
        @DisplayName("위시 추가/제거 가능 상태 계산")
        fun `should compute add and remove capabilities correctly`() = runTest {
            // Initially - can add, cannot remove (only 1 wish)
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.canAddMoreWishes shouldBe true
                state.canRemoveWishes shouldBe false
            }
            
            // Add wishes to reach maximum
            viewModel.onEvent(WishInputEvent.AddWish())
            viewModel.onEvent(WishInputEvent.AddWish())
            advanceUntilIdle()
            
            // Then - cannot add (3 wishes), can remove
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.canAddMoreWishes shouldBe false
                state.canRemoveWishes shouldBe true
            }
        }
    }
    
    @Nested
    @DisplayName("네비게이션 및 효과 테스트")
    inner class NavigationEffectTests {
        
        @Test
        @DisplayName("뒤로가기 네비게이션")
        fun `should navigate back`() = runTest {
            // When
            viewModel.onEvent(WishInputEvent.NavigateBack)
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.NavigateBack>()
            }
        }
        
        @Test
        @DisplayName("위시 검증 실행")
        fun `should validate wishes`() = runTest {
            // When - Validate with empty wishes
            viewModel.onEvent(WishInputEvent.ValidateWish)
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowValidationError>()
                val validationEffect = effect as WishInputEffect.ShowValidationError
                validationEffect.field shouldBe ValidationField.WISH_TEXT
            }
        }
        
        @Test
        @DisplayName("에러 메시지 해제")
        fun `should dismiss error message`() = runTest {
            // Given - Set error state
            viewModel.uiState.value = viewModel.uiState.value.copy(error = "테스트 에러")
            
            // When
            viewModel.onEvent(WishInputEvent.DismissError)
            advanceUntilIdle()
            
            // Then
            viewModel.uiState.test {
                val state = expectMostRecentItem()
                state.error shouldBe null
            }
        }
        
        @Test
        @DisplayName("목표 횟수 선택기 표시")
        fun `should show target count picker`() = runTest {
            // When
            viewModel.onEvent(WishInputEvent.ShowTargetCountPicker)
            advanceUntilIdle()
            
            // Then
            viewModel.effect.test {
                val effect = expectMostRecentItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowNumberPicker>()
            }
        }
    }
}