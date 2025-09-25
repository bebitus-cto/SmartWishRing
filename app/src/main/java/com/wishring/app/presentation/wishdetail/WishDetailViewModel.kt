package com.wishring.app.presentation.wishdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.repository.WishRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Simplified ViewModel for WishDetail screen
 * Focused only on displaying wish data for a specific date
 */
@HiltViewModel
class WishDetailViewModel @Inject constructor(
    private val wishRepository: WishRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        WishDetailViewState(
            selectedDate = savedStateHandle.get<String>("date")?.let { 
                LocalDate.parse(it) 
            } ?: LocalDate.now()
        )
    )
    val uiState: StateFlow<WishDetailViewState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WishDetailEffect>()
    val effect = _effect.asSharedFlow()

    init {
        loadDataForCurrentDate()
    }

    fun onEvent(event: WishDetailEvent) {
        when (event) {
            is WishDetailEvent.NavigateBack -> navigateBack()
            is WishDetailEvent.LoadDataForDate -> loadDataForDate(event.date)
            is WishDetailEvent.NavigateToPreviousDate -> navigateToPreviousDate()
            is WishDetailEvent.NavigateToNextDate -> navigateToNextDate()
            is WishDetailEvent.RetryLoading -> loadDataForCurrentDate()
            is WishDetailEvent.DismissError -> dismissError()
        }
    }

    private fun loadDataForCurrentDate() {
        loadDataForDate(_uiState.value.selectedDate)
    }

    private fun loadDataForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    selectedDate = date,
                    error = null
                ) 
            }

            try {
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val wishDay = wishRepository.getWishDay(dateString)

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        targetCount = wishDay?.targetCount ?: 0,
                        wishText = wishDay?.wishText ?: "",
                        motivationalMessages = getMotivationalMessages()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "데이터를 불러오는 중 오류가 발생했습니다"
                    ) 
                }
            }
        }
    }

    private fun navigateToPreviousDate() {
        val previousDate = _uiState.value.selectedDate.minusDays(1)
        loadDataForDate(previousDate)
    }

    private fun navigateToNextDate() {
        val nextDate = _uiState.value.selectedDate.plusDays(1)
        if (nextDate <= LocalDate.now()) {
            loadDataForDate(nextDate)
        }
    }

    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(WishDetailEffect.NavigateBack)
        }
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun getMotivationalMessages(): List<String> {
        return listOf(
            "나는 어제보다 더 나은 내가 되고 있다.",
            "오늘의 선택이 나를 더 단단하게 만든다.",
            "내 안의 가능성은 멈추지 않고 자라고 있다."
        )
    }
}