package com.wishring.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.domain.repository.BleRepository
import com.wishring.app.presentation.base.BaseViewModel
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
import org.junit.jupiter.params.provider.CsvSource
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.string.shouldContain
import app.cash.turbine.test
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
@DisplayName("WishInputViewModel 테스트")
class WishInputViewModelTest {

    @MockK
    private lateinit var wishCountRepository: WishCountRepository
    
    @MockK
    private lateinit var preferencesRepository: PreferencesRepository
    
    @MockK
    private lateinit var bleRepository: BleRepository
    
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle
    
    private lateinit var viewModel: WishInputViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        
        // Default mocks
        every { savedStateHandle.get<Any>(any()) } returns null
        every { savedStateHandle.set(any(), any<Any>()) } just Runs
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("위시 입력 기본 기능 테스트")
    inner class BasicWishInputTests {
        
        @Test
        @DisplayName("텍스트 위시 입력")
        fun `should input text wish`() = runTest {
            // Given
            setupDefaultMocks()
            val wishText = "오늘도 행복한 하루 되기"
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText(wishText))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe wishText
                state.isValidInput shouldBe true
            }
        }
        
        @Test
        @DisplayName("빈 위시 텍스트 검증")
        fun `should validate empty wish text`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText(""))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe ""
                state.isValidInput shouldBe false
                state.validationError shouldBe "위시를 입력해주세요"
            }
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 5, 10, 20, 50])
        @DisplayName("카운트 증가량 설정")
        fun `should set count increment amount`(amount: Int) = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.SetIncrementAmount(amount))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.incrementAmount shouldBe amount
            }
        }
        
        @Test
        @DisplayName("위시 제출 성공")
        fun `should submit wish successfully`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.incrementCount(any()) } just Runs
            coEvery { wishCountRepository.saveWishText(any(), any()) } just Runs
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText("테스트 위시"))
            viewModel.handleEvent(WishInputEvent.SubmitWish)
            advanceUntilIdle()
            
            // Then
            coVerify { 
                wishCountRepository.incrementCount(1)
                wishCountRepository.saveWishText(LocalDate.now(), "테스트 위시")
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.WishSubmitted>()
            }
        }
    }
    
    @Nested
    @DisplayName("음성 입력 테스트")
    inner class VoiceInputTests {
        
        @Test
        @DisplayName("음성 입력 시작")
        fun `should start voice input`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.StartVoiceInput)
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isListening shouldBe true
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.StartListening>()
            }
        }
        
        @Test
        @DisplayName("음성 입력 중지")
        fun `should stop voice input`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { copy(isListening = true) }
            
            // When
            viewModel.handleEvent(WishInputEvent.StopVoiceInput)
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isListening shouldBe false
            }
        }
        
        @Test
        @DisplayName("음성 인식 결과 처리")
        fun `should process voice recognition result`() = runTest {
            // Given
            setupDefaultMocks()
            val recognizedText = "음성으로 입력한 위시"
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.OnVoiceResult(recognizedText))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe recognizedText
                state.isListening shouldBe false
            }
        }
        
        @Test
        @DisplayName("음성 인식 에러 처리")
        fun `should handle voice recognition error`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { copy(isListening = true) }
            
            // When
            viewModel.handleEvent(WishInputEvent.OnVoiceError("Recognition failed"))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isListening shouldBe false
                state.voiceError shouldBe "Recognition failed"
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowError>()
            }
        }
    }
    
    @Nested
    @DisplayName("위시 템플릿 테스트")
    inner class WishTemplateTests {
        
        @Test
        @DisplayName("템플릿 목록 로드")
        fun `should load wish templates`() = runTest {
            // Given
            val templates = listOf(
                WishTemplate(1, "오늘도 감사한 하루"),
                WishTemplate(2, "건강하고 행복한 하루"),
                WishTemplate(3, "모든 일이 잘 풀리는 하루")
            )
            
            coEvery { preferencesRepository.getWishTemplates() } returns templates
            setupDefaultMocks()
            
            // When
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.handleEvent(WishInputEvent.LoadTemplates)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.templates shouldBe templates
            }
        }
        
        @Test
        @DisplayName("템플릿 선택")
        fun `should select wish template`() = runTest {
            // Given
            val template = WishTemplate(1, "오늘도 감사한 하루")
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { 
                copy(templates = listOf(template))
            }
            
            // When
            viewModel.handleEvent(WishInputEvent.SelectTemplate(template))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe template.text
                state.selectedTemplate shouldBe template
            }
        }
        
        @Test
        @DisplayName("커스텀 템플릿 저장")
        fun `should save custom template`() = runTest {
            // Given
            val customText = "나만의 특별한 위시"
            setupDefaultMocks()
            coEvery { preferencesRepository.saveWishTemplate(customText) } just Runs
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { copy(wishText = customText) }
            
            // When
            viewModel.handleEvent(WishInputEvent.SaveAsTemplate)
            advanceUntilIdle()
            
            // Then
            coVerify { preferencesRepository.saveWishTemplate(customText) }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.TemplateSaved>()
            }
        }
    }
    
    @Nested
    @DisplayName("반복 위시 설정 테스트")
    inner class RepeatWishTests {
        
        @Test
        @DisplayName("반복 위시 활성화")
        fun `should enable repeat wish`() = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.SetRepeatEnabled(true))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.isRepeatEnabled shouldBe true
            }
        }
        
        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5, 10])
        @DisplayName("반복 횟수 설정")
        fun `should set repeat count`(count: Int) = runTest {
            // Given
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.SetRepeatCount(count))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.repeatCount shouldBe count
            }
        }
        
        @Test
        @DisplayName("반복 위시 제출")
        fun `should submit repeated wish`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.incrementCount(any()) } just Runs
            coEvery { wishCountRepository.saveWishText(any(), any()) } just Runs
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState {
                copy(
                    wishText = "반복 위시",
                    isRepeatEnabled = true,
                    repeatCount = 3
                )
            }
            
            // When
            viewModel.handleEvent(WishInputEvent.SubmitWish)
            advanceUntilIdle()
            
            // Then
            coVerify(exactly = 3) { 
                wishCountRepository.incrementCount(1)
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.RepeatWishCompleted>()
                (effect as WishInputEffect.RepeatWishCompleted).count shouldBe 3
            }
        }
    }
    
    @Nested
    @DisplayName("BLE 연동 입력 테스트")
    inner class BleInputTests {
        
        @Test
        @DisplayName("BLE 버튼으로 위시 입력")
        fun `should input wish via BLE button`() = runTest {
            // Given
            val buttonEvents = MutableSharedFlow<BleButtonEvent>()
            
            setupDefaultMocks()
            every { bleRepository.buttonEvents } returns buttonEvents
            coEvery { wishCountRepository.incrementCount(any()) } just Runs
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { copy(wishText = "BLE 위시") }
            
            // When
            buttonEvents.emit(BleButtonEvent.SinglePress)
            advanceUntilIdle()
            
            // Then
            coVerify { wishCountRepository.incrementCount(1) }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.BleWishSubmitted>()
            }
        }
        
        @Test
        @DisplayName("BLE 연결 상태 표시")
        fun `should show BLE connection status`() = runTest {
            // Given
            val connectionState = MutableStateFlow<BleConnectionState>(
                BleConnectionState.Disconnected
            )
            
            setupDefaultMocks()
            every { bleRepository.connectionState } returns connectionState
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // Then - Initially disconnected
            viewModel.viewState.test {
                expectMostRecentItem().isBleConnected shouldBe false
            }
            
            // When - Connect
            connectionState.value = BleConnectionState.Connected("device")
            advanceUntilIdle()
            
            // Then - Connected
            viewModel.viewState.test {
                expectMostRecentItem().isBleConnected shouldBe true
            }
        }
    }
    
    @Nested
    @DisplayName("위시 히스토리 테스트")
    inner class WishHistoryTests {
        
        @Test
        @DisplayName("최근 위시 목록 로드")
        fun `should load recent wishes`() = runTest {
            // Given
            val recentWishes = listOf(
                WishEntry(1, LocalDate.now(), "오늘의 위시 1", LocalTime.of(10, 0)),
                WishEntry(2, LocalDate.now(), "오늘의 위시 2", LocalTime.of(14, 30)),
                WishEntry(3, LocalDate.now(), "오늘의 위시 3", LocalTime.of(18, 15))
            )
            
            coEvery { wishCountRepository.getRecentWishes(10) } returns recentWishes
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.LoadRecentWishes)
            advanceUntilIdle()
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.recentWishes shouldBe recentWishes
            }
        }
        
        @Test
        @DisplayName("위시 재사용")
        fun `should reuse previous wish`() = runTest {
            // Given
            val previousWish = WishEntry(1, LocalDate.now(), "재사용할 위시", LocalTime.of(10, 0))
            setupDefaultMocks()
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.ReusePreviousWish(previousWish))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe previousWish.text
            }
        }
    }
    
    @Nested
    @DisplayName("입력 제한 및 검증 테스트")
    inner class ValidationTests {
        
        @Test
        @DisplayName("최대 길이 제한")
        fun `should limit wish text length`() = runTest {
            // Given
            setupDefaultMocks()
            val longText = "a".repeat(201) // Max length is 200
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText(longText))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText.length shouldBe 200
                state.validationError shouldBe "최대 200자까지 입력 가능합니다"
            }
        }
        
        @Test
        @DisplayName("특수문자 필터링")
        fun `should filter special characters if enabled`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { preferencesRepository.isSpecialCharFilterEnabled() } returns true
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText("위시@#$%테스트"))
            
            // Then
            viewModel.viewState.test {
                val state = expectMostRecentItem()
                state.wishText shouldBe "위시테스트"
            }
        }
        
        @Test
        @DisplayName("연속 제출 방지")
        fun `should prevent rapid submissions`() = runTest {
            // Given
            setupDefaultMocks()
            coEvery { wishCountRepository.incrementCount(any()) } just Runs
            coEvery { wishCountRepository.saveWishText(any(), any()) } just Runs
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            viewModel.updateState { copy(wishText = "테스트") }
            
            // When - Submit twice rapidly
            viewModel.handleEvent(WishInputEvent.SubmitWish)
            viewModel.handleEvent(WishInputEvent.SubmitWish)
            advanceUntilIdle()
            
            // Then - Only one submission should occur
            coVerify(exactly = 1) { 
                wishCountRepository.incrementCount(any())
            }
            
            viewModel.effect.test {
                val effect = awaitItem()
                effect.shouldBeInstanceOf<WishInputEffect.ShowError>()
                (effect as WishInputEffect.ShowError).message shouldContain "잠시"
            }
        }
    }
    
    @Nested
    @DisplayName("상태 저장 및 복원 테스트")
    inner class StateRestorationTests {
        
        @Test
        @DisplayName("입력 중인 텍스트 저장")
        fun `should save draft wish text`() = runTest {
            // Given
            setupDefaultMocks()
            val draftText = "저장할 위시 텍스트"
            
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // When
            viewModel.handleEvent(WishInputEvent.UpdateWishText(draftText))
            
            // Then
            verify { savedStateHandle.set("draft_wish", draftText) }
        }
        
        @Test
        @DisplayName("저장된 텍스트 복원")
        fun `should restore draft wish text`() = runTest {
            // Given
            val savedDraft = "이전에 입력한 위시"
            every { savedStateHandle.get<String>("draft_wish") } returns savedDraft
            setupDefaultMocks()
            
            // When
            viewModel = WishInputViewModel(
                wishCountRepository = wishCountRepository,
                preferencesRepository = preferencesRepository,
                bleRepository = bleRepository,
                savedStateHandle = savedStateHandle
            )
            
            // Then
            viewModel.viewState.test {
                val state = awaitItem()
                state.wishText shouldBe savedDraft
            }
        }
    }
    
    // Helper functions
    private fun setupDefaultMocks() {
        coEvery { wishCountRepository.getRecentWishes(any()) } returns emptyList()
        coEvery { preferencesRepository.getWishTemplates() } returns emptyList()
        coEvery { preferencesRepository.isSpecialCharFilterEnabled() } returns false
        every { bleRepository.connectionState } returns flowOf(BleConnectionState.Disconnected)
        every { bleRepository.buttonEvents } returns emptyFlow()
    }
}

// Mock ViewModel implementation
class WishInputViewModel(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository,
    private val bleRepository: BleRepository,
    private val savedStateHandle: SavedStateHandle
) : BaseViewModel<WishInputViewState, WishInputEvent, WishInputEffect>(
    initialState = WishInputViewState()
) {
    
    private var lastSubmitTime = 0L
    private val SUBMIT_COOLDOWN = 1000L // 1 second
    
    init {
        restoreDraft()
        observeBleEvents()
    }
    
    override fun handleEvent(event: WishInputEvent) {
        when (event) {
            is WishInputEvent.UpdateWishText -> updateWishText(event.text)
            is WishInputEvent.SetIncrementAmount -> updateState { copy(incrementAmount = event.amount) }
            is WishInputEvent.SubmitWish -> submitWish()
            is WishInputEvent.StartVoiceInput -> startVoiceInput()
            is WishInputEvent.StopVoiceInput -> stopVoiceInput()
            is WishInputEvent.OnVoiceResult -> handleVoiceResult(event.text)
            is WishInputEvent.OnVoiceError -> handleVoiceError(event.error)
            is WishInputEvent.LoadTemplates -> loadTemplates()
            is WishInputEvent.SelectTemplate -> selectTemplate(event.template)
            is WishInputEvent.SaveAsTemplate -> saveAsTemplate()
            is WishInputEvent.SetRepeatEnabled -> updateState { copy(isRepeatEnabled = event.enabled) }
            is WishInputEvent.SetRepeatCount -> updateState { copy(repeatCount = event.count) }
            is WishInputEvent.LoadRecentWishes -> loadRecentWishes()
            is WishInputEvent.ReusePreviousWish -> reusePreviousWish(event.wish)
        }
    }
    
    private fun updateWishText(text: String) {
        launch {
            var processedText = text
            
            // Apply length limit
            if (text.length > 200) {
                processedText = text.take(200)
                updateState { copy(validationError = "최대 200자까지 입력 가능합니다") }
            }
            
            // Apply special character filter if enabled
            if (preferencesRepository.isSpecialCharFilterEnabled()) {
                processedText = processedText.replace(Regex("[^가-힣a-zA-Z0-9\\s]"), "")
            }
            
            // Update state
            updateState { 
                copy(
                    wishText = processedText,
                    isValidInput = processedText.isNotBlank(),
                    validationError = if (processedText.isBlank()) "위시를 입력해주세요" else null
                )
            }
            
            // Save draft
            savedStateHandle.set("draft_wish", processedText)
        }
    }
    
    private fun submitWish() {
        val currentTime = System.currentTimeMillis()
        
        // Check cooldown
        if (currentTime - lastSubmitTime < SUBMIT_COOLDOWN) {
            sendEffect(WishInputEffect.ShowError("잠시 후 다시 시도해주세요"))
            return
        }
        
        val state = viewState.value
        
        if (!state.isValidInput) {
            sendEffect(WishInputEffect.ShowError("위시를 입력해주세요"))
            return
        }
        
        launch {
            updateState { copy(isSubmitting = true) }
            
            try {
                if (state.isRepeatEnabled) {
                    repeat(state.repeatCount) {
                        wishCountRepository.incrementCount(state.incrementAmount)
                        delay(100) // Small delay between repeats
                    }
                    wishCountRepository.saveWishText(LocalDate.now(), state.wishText)
                    sendEffect(WishInputEffect.RepeatWishCompleted(state.repeatCount))
                } else {
                    wishCountRepository.incrementCount(state.incrementAmount)
                    wishCountRepository.saveWishText(LocalDate.now(), state.wishText)
                    sendEffect(WishInputEffect.WishSubmitted)
                }
                
                // Clear draft after successful submission
                savedStateHandle.set("draft_wish", "")
                updateState { copy(wishText = "", isSubmitting = false) }
                
                lastSubmitTime = currentTime
            } catch (e: Exception) {
                updateState { copy(isSubmitting = false) }
                sendEffect(WishInputEffect.ShowError(e.message ?: "제출 실패"))
            }
        }
    }
    
    private fun startVoiceInput() {
        updateState { copy(isListening = true) }
        sendEffect(WishInputEffect.StartListening)
    }
    
    private fun stopVoiceInput() {
        updateState { copy(isListening = false) }
    }
    
    private fun handleVoiceResult(text: String) {
        updateState { 
            copy(
                wishText = text,
                isListening = false,
                voiceError = null
            )
        }
    }
    
    private fun handleVoiceError(error: String) {
        updateState { 
            copy(
                isListening = false,
                voiceError = error
            )
        }
        sendEffect(WishInputEffect.ShowError(error))
    }
    
    private fun loadTemplates() {
        launch {
            val templates = preferencesRepository.getWishTemplates()
            updateState { copy(templates = templates) }
        }
    }
    
    private fun selectTemplate(template: WishTemplate) {
        updateState { 
            copy(
                wishText = template.text,
                selectedTemplate = template
            )
        }
    }
    
    private fun saveAsTemplate() {
        val text = viewState.value.wishText
        if (text.isBlank()) return
        
        launch {
            preferencesRepository.saveWishTemplate(text)
            sendEffect(WishInputEffect.TemplateSaved)
        }
    }
    
    private fun loadRecentWishes() {
        launch {
            val recent = wishCountRepository.getRecentWishes(10)
            updateState { copy(recentWishes = recent) }
        }
    }
    
    private fun reusePreviousWish(wish: WishEntry) {
        updateState { copy(wishText = wish.text) }
    }
    
    private fun restoreDraft() {
        val draft = savedStateHandle.get<String>("draft_wish")
        if (!draft.isNullOrBlank()) {
            updateState { copy(wishText = draft) }
        }
    }
    
    private fun observeBleEvents() {
        bleRepository.connectionState
            .onEach { state ->
                updateState { 
                    copy(isBleConnected = state is BleConnectionState.Connected)
                }
            }
            .launchIn(viewModelScope)
        
        bleRepository.buttonEvents
            .onEach { event ->
                if (event is BleButtonEvent.SinglePress && viewState.value.wishText.isNotBlank()) {
                    submitWish()
                    sendEffect(WishInputEffect.BleWishSubmitted)
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun updateState(update: WishInputViewState.() -> WishInputViewState) {
        _viewState.value = viewState.value.update()
    }
}

// Supporting classes
data class WishInputViewState(
    val wishText: String = "",
    val incrementAmount: Int = 1,
    val isValidInput: Boolean = false,
    val validationError: String? = null,
    val isSubmitting: Boolean = false,
    val isListening: Boolean = false,
    val voiceError: String? = null,
    val templates: List<WishTemplate> = emptyList(),
    val selectedTemplate: WishTemplate? = null,
    val isRepeatEnabled: Boolean = false,
    val repeatCount: Int = 1,
    val recentWishes: List<WishEntry> = emptyList(),
    val isBleConnected: Boolean = false
)

sealed class WishInputEvent {
    data class UpdateWishText(val text: String) : WishInputEvent()
    data class SetIncrementAmount(val amount: Int) : WishInputEvent()
    object SubmitWish : WishInputEvent()
    object StartVoiceInput : WishInputEvent()
    object StopVoiceInput : WishInputEvent()
    data class OnVoiceResult(val text: String) : WishInputEvent()
    data class OnVoiceError(val error: String) : WishInputEvent()
    object LoadTemplates : WishInputEvent()
    data class SelectTemplate(val template: WishTemplate) : WishInputEvent()
    object SaveAsTemplate : WishInputEvent()
    data class SetRepeatEnabled(val enabled: Boolean) : WishInputEvent()
    data class SetRepeatCount(val count: Int) : WishInputEvent()
    object LoadRecentWishes : WishInputEvent()
    data class ReusePreviousWish(val wish: WishEntry) : WishInputEvent()
}

sealed class WishInputEffect {
    object WishSubmitted : WishInputEffect()
    object StartListening : WishInputEffect()
    data class ShowError(val message: String) : WishInputEffect()
    object TemplateSaved : WishInputEffect()
    data class RepeatWishCompleted(val count: Int) : WishInputEffect()
    object BleWishSubmitted : WishInputEffect()
}

data class WishTemplate(
    val id: Long,
    val text: String
)

data class WishEntry(
    val id: Long,
    val date: LocalDate,
    val text: String,
    val time: LocalTime
)

sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Connected(val deviceAddress: String) : BleConnectionState()
}

sealed class BleButtonEvent {
    object SinglePress : BleButtonEvent()
}