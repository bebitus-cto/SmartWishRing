package com.wishring.app.domain.usecase

import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.repository.WishRepository
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

/**
 * Unit tests for IncrementDailyCountUseCase
 * Tests the business logic for incrementing shared daily count
 */
class IncrementDailyCountUseCaseTest {

    @Mock
    private lateinit var wishRepository: WishRepository
    
    private lateinit var useCase: IncrementDailyCountUseCase
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        useCase = IncrementDailyCountUseCase(wishRepository)
    }
    
    @Test
    fun `execute should increment daily count successfully`() = runTest {
        // Given
        val amount = 1
        val expectedWishUiState = WishUiState.createDefault().copy(targetCount = 1)
        whenever(wishRepository.incrementTodayCount(amount))
            .thenReturn(expectedWishUiState)
        
        // When
        val result = useCase.execute(amount)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedWishUiState, result.getOrNull())
        verify(wishRepository).incrementTodayCount(amount)
    }
    
    @Test
    fun `execute should handle repository exception`() = runTest {
        // Given
        val amount = 1
        val exception = RuntimeException("Database error")
        whenever(wishRepository.incrementTodayCount(amount))
            .thenThrow(exception)
        
        // When
        val result = useCase.execute(amount)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(wishRepository).incrementTodayCount(amount)
    }
    
    @Test
    fun `execute should use default amount when not specified`() = runTest {
        // Given
        val expectedWishUiState = WishUiState.createDefault().copy(targetCount = 1)
        whenever(wishRepository.incrementTodayCount(1))
            .thenReturn(expectedWishUiState)
        
        // When
        val result = useCase.execute() // No amount specified, should default to 1
        
        // Then
        assertTrue(result.isSuccess)
        verify(wishRepository).incrementTodayCount(1)
    }
    
    @Test
    fun `execute should handle large increment amounts`() = runTest {
        // Given
        val amount = 50
        val expectedWishUiState = WishUiState.createDefault().copy(targetCount = 50)
        whenever(wishRepository.incrementTodayCount(amount))
            .thenReturn(expectedWishUiState)
        
        // When
        val result = useCase.execute(amount)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedWishUiState, result.getOrNull())
        verify(wishRepository).incrementTodayCount(amount)
    }
    
    @Test
    fun `execute should handle zero increment gracefully`() = runTest {
        // Given
        val amount = 0
        val expectedWishUiState = WishUiState.createDefault().copy(targetCount = 0)
        whenever(wishRepository.incrementTodayCount(amount))
            .thenReturn(expectedWishUiState)
        
        // When
        val result = useCase.execute(amount)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedWishUiState, result.getOrNull())
        verify(wishRepository).incrementTodayCount(amount)
    }
    
    @Test
    fun `execute should handle negative increment amounts`() = runTest {
        // Given
        val amount = -1
        val expectedWishUiState = WishUiState.createDefault().copy(targetCount = 0) // Assuming repository handles negative properly
        whenever(wishRepository.incrementTodayCount(amount))
            .thenReturn(expectedWishUiState)
        
        // When
        val result = useCase.execute(amount)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedWishUiState, result.getOrNull())
        verify(wishRepository).incrementTodayCount(amount)
    }
}