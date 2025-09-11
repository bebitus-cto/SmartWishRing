package com.wishring.app.presentation.wishinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for WishInput screen
 * Manages wish input state and business logic
 */
@HiltViewModel
class WishInputViewModel @Inject constructor(
    private val wishCountRepository: WishCountRepository,
    private val preferencesRepository: PreferencesRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WishInputViewState())
    val uiState: StateFlow<WishInputViewState> = _uiState.asStateFlow()
    
    private val _effect = Channel<WishInputEffect>(Channel.BUFFERED)
    val effect: Flow<WishInputEffect> = _effect.receiveAsFlow()
    
    init {
        loadDefaults()
        checkForExistingRecord()
    }
    
    /**
     * Handle user events
     */
    fun onEvent(event: WishInputEvent) {
        when (event) {
            is WishInputEvent.UpdateWishText -> updateWishText(event.text)
            is WishInputEvent.UpdateTargetCount -> updateTargetCount(event.count)
            is WishInputEvent.SelectSuggestedWish -> selectSuggestedWish(event.wish)
            WishInputEvent.ToggleSuggestions -> toggleSuggestions()
            WishInputEvent.ShowTargetCountPicker -> showTargetCountPicker()
            WishInputEvent.HideTargetCountPicker -> hideTargetCountPicker()
            WishInputEvent.SaveWish -> saveWish()
            WishInputEvent.DeleteWish -> deleteWish()
            WishInputEvent.ShowDeleteConfirmation -> showDeleteConfirmation()
            WishInputEvent.HideDeleteConfirmation -> hideDeleteConfirmation()
            WishInputEvent.ConfirmDelete -> confirmDelete()
            WishInputEvent.NavigateBack -> navigateBack()
            WishInputEvent.ClearWishText -> clearWishText()
            WishInputEvent.ResetToDefaults -> resetToDefaults()
            is WishInputEvent.LoadExistingRecord -> loadExistingRecord(event.date)
            WishInputEvent.DismissError -> dismissError()
            WishInputEvent.ValidateWish -> validateWish()
        }
    }
    
    private fun loadDefaults() {
        viewModelScope.launch {
            try {
                val defaultWish = preferencesRepository.getDefaultWishText()
                val defaultTarget = preferencesRepository.getDefaultTargetCount()
                
                _uiState.update { state ->
                    state.copy(
                        wishText = defaultWish,
                        targetCount = defaultTarget
                    )
                }
            } catch (e: Exception) {
                // Use default values from state
            }
        }
    }
    
    private fun checkForExistingRecord() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val record = wishCountRepository.getDailyRecord(today.toString())
            if (record != null) {
                _uiState.update { state ->
                    state.copy(
                        wishText = record.wishText,
                        targetCount = record.targetCount,
                        isEditMode = true,
                        existingRecord = true
                    )
                }
            }
        }
    }
    
    private fun updateWishText(text: String) {
        if (text.length <= _uiState.value.maxWishLength) {
            _uiState.update { state ->
                state.copy(
                    wishText = text,
                    isWishValid = text.isNotBlank()
                )
            }
        } else {
            viewModelScope.launch {
                _effect.send(WishInputEffect.ShowValidationError(
                    ValidationField.WISH_TEXT,
                    "최대 ${_uiState.value.maxWishLength}자까지 입력 가능합니다"
                ))
            }
        }
    }
    
    private fun updateTargetCount(count: Int) {
        if (count in _uiState.value.minTargetCount.._uiState.value.maxTargetCount) {
            _uiState.update { state ->
                state.copy(targetCount = count)
            }
        }
    }
    
    private fun selectSuggestedWish(wish: String) {
        _uiState.update { state ->
            state.copy(
                wishText = wish,
                showSuggestions = false,
                isWishValid = true
            )
        }
        
        viewModelScope.launch {
            _effect.send(WishInputEffect.VibrateForFeedback)
        }
    }
    
    private fun toggleSuggestions() {
        _uiState.update { state ->
            state.copy(showSuggestions = !state.showSuggestions)
        }
    }
    
    private fun showTargetCountPicker() {
        viewModelScope.launch {
            _effect.send(WishInputEffect.ShowNumberPicker(
                currentValue = _uiState.value.targetCount,
                minValue = _uiState.value.minTargetCount,
                maxValue = _uiState.value.maxTargetCount,
                onValueSelected = { count ->
                    onEvent(WishInputEvent.UpdateTargetCount(count))
                }
            ))
        }
    }
    
    private fun hideTargetCountPicker() {
        _uiState.update { state ->
            state.copy(showTargetCountPicker = false)
        }
    }
    
    private fun saveWish() {
        if (!_uiState.value.isSaveEnabled) {
            viewModelScope.launch {
                _effect.send(WishInputEffect.ShowValidationError(
                    ValidationField.WISH_TEXT,
                    "위시 텍스트를 입력해주세요"
                ))
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true) }
            
            try {
                val today = LocalDate.now()
                // Save wish using repository method
                wishCountRepository.updateTodayWishAndTarget(
                    wishText = _uiState.value.wishText,
                    targetCount = _uiState.value.targetCount
                )
                
                _effect.send(WishInputEffect.ShowToast(
                    if (_uiState.value.isEditMode) "위시가 수정되었습니다" else "위시가 생성되었습니다"
                ))
                
                _effect.send(WishInputEffect.NavigateToHomeWithResult("위시가 저장되었습니다"))
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        error = "위시 저장에 실패했습니다: ${e.message}",
                        isSaving = false
                    )
                }
            }
        }
    }
    
    private fun deleteWish() {
        showDeleteConfirmation()
    }
    
    private fun showDeleteConfirmation() {
        _uiState.update { state ->
            state.copy(showDeleteConfirmation = true)
        }
    }
    
    private fun hideDeleteConfirmation() {
        _uiState.update { state ->
            state.copy(showDeleteConfirmation = false)
        }
    }
    
    private fun confirmDelete() {
        viewModelScope.launch {
            try {
                // Clear today's wish by setting empty text and 0 count
                val today = LocalDate.now().toString()
                val existing = wishCountRepository.getWishCountByDate(today)
                if (existing != null) {
                    wishCountRepository.saveWishCount(existing.copy(totalCount = 0, wishText = ""))
                }
                
                _effect.send(WishInputEffect.ShowToast("위시가 삭제되었습니다"))
                _effect.send(WishInputEffect.NavigateBack)
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        error = "위시 삭제에 실패했습니다: ${e.message}",
                        showDeleteConfirmation = false
                    )
                }
            }
        }
    }
    
    private fun navigateBack() {
        viewModelScope.launch {
            _effect.send(WishInputEffect.NavigateBack)
        }
    }
    
    private fun clearWishText() {
        _uiState.update { state ->
            state.copy(
                wishText = "",
                isWishValid = false
            )
        }
    }
    
    private fun resetToDefaults() {
        loadDefaults()
    }
    
    private fun loadExistingRecord(date: String) {
        viewModelScope.launch {
            try {
                val recordDate = LocalDate.parse(date)
                val record = wishCountRepository.getDailyRecord(recordDate.toString())
                if (record != null) {
                    _uiState.update { state ->
                        state.copy(
                            wishText = record.wishText,
                            targetCount = record.targetCount,
                            isEditMode = true,
                            existingRecord = true,
                            date = date
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(error = "기록을 불러올 수 없습니다")
                }
            }
        }
    }
    
    private fun dismissError() {
        _uiState.update { state ->
            state.copy(error = null)
        }
    }
    
    private fun validateWish() {
        val wishText = _uiState.value.wishText
        val isValid = wishText.isNotBlank() && wishText.length <= _uiState.value.maxWishLength
        
        _uiState.update { state ->
            state.copy(isWishValid = isValid)
        }
        
        if (!isValid) {
            viewModelScope.launch {
                _effect.send(WishInputEffect.ShowValidationError(
                    ValidationField.WISH_TEXT,
                    if (wishText.isBlank()) "위시를 입력해주세요" else "위시가 너무 깁니다"
                ))
            }
        }
    }
}