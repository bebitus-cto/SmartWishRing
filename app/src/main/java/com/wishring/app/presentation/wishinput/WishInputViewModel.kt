package com.wishring.app.presentation.wishinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.data.repository.WishRepository
import com.wishring.app.data.repository.PreferencesRepository
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
    private val wishRepository: WishRepository,
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
     * Handle user events (supports multiple wishes)
     */
    fun onEvent(event: WishInputEvent) {
        when (event) {
            is WishInputEvent.AddWish -> addWish(event.position)
            is WishInputEvent.RemoveWish -> removeWish(event.index)
            is WishInputEvent.UpdateWishText -> updateWishText(event.index, event.text)
            is WishInputEvent.UpdateWishCount -> updateWishCount(event.index, event.count)
            WishInputEvent.SaveWish -> saveWish()
            WishInputEvent.DeleteWish -> deleteWish()
            WishInputEvent.ShowDeleteConfirmation -> showDeleteConfirmation()
            WishInputEvent.HideDeleteConfirmation -> hideDeleteConfirmation()
            WishInputEvent.ConfirmDelete -> confirmDelete()
            WishInputEvent.NavigateBack -> navigateBack()
            WishInputEvent.ResetToDefaults -> resetToDefaults()
            is WishInputEvent.LoadExistingRecord -> loadExistingRecord(event.date)
            WishInputEvent.DismissError -> dismissError()
            WishInputEvent.ClearWishText -> clearWishText()
            WishInputEvent.ValidateWish -> validateWish()
        }
    }
    
    private fun loadDefaults() {
        viewModelScope.launch {
            try {
                val defaultWish = preferencesRepository.getDefaultWishText()
                val defaultTarget = preferencesRepository.getDefaultTargetCount()
                
                // Create initial wish item with defaults
                val initialWish = WishDayUiState(
                    date = LocalDate.now(),
                    wishText = defaultWish,
                    isCompleted = false,
                    targetCount = defaultTarget,
                    completedCount = 0
                )
                _uiState.update { state ->
                    state.copy(
                        wishes = listOf(initialWish)
                    )
                }
            } catch (e: Exception) {
                // Use default values from state (empty wish)
            }
        }
    }
    
    private fun checkForExistingRecord() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val today = LocalDate.now()
                val todayWish = wishRepository.getTodayWish()
                
                if (todayWish != null && todayWish.wishText.isNotBlank()) {
                    // Load existing wish
                    val existingWish = WishDayUiState(
                        date = today,
                        wishText = todayWish.wishText,
                        isCompleted = todayWish.isCompleted,
                        targetCount = todayWish.targetCount,
                        completedCount = todayWish.currentCount
                    )
                    _uiState.update { state ->
                        state.copy(
                            wishes = listOf(existingWish),
                            isEditMode = true,
                            existingRecord = true,
                            isLoading = false
                        )
                    }
                } else {
                    // No existing wish
                    _uiState.update { it.copy(isLoading = false) }
                }
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "기존 위시를 불러오는데 실패했습니다"
                    )
                }
            }
        }
    }
    
    private fun addWish(position: Int? = null) {
        val currentState = _uiState.value
        if (currentState.canAddMoreWishes) {
            val newWish = WishDayUiState.empty(LocalDate.now())
            val updatedWishes = if (position != null && position <= currentState.wishes.size) {
                currentState.wishes.toMutableList().apply {
                    add(position, newWish)
                }
            } else {
                currentState.wishes + newWish
            }
            
            _uiState.update { state ->
                state.copy(wishes = updatedWishes)
            }
        }
    }
    
    private fun removeWish(index: Int) {
        val currentState = _uiState.value
        if (currentState.canRemoveWishes && index in currentState.wishes.indices) {
            val updatedWishes = currentState.wishes.toMutableList().apply {
                removeAt(index)
            }
            _uiState.update { state ->
                state.copy(wishes = updatedWishes)
            }
        }
    }
    
    private fun updateWishText(index: Int, text: String) {
        if (text.length <= _uiState.value.maxWishLength) {
            _uiState.update { state ->
                val updatedWishes = state.wishes.mapIndexed { i, wish ->
                    if (i == index) wish.copy(wishText = text) else wish
                }
                state.copy(wishes = updatedWishes)
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
    
    private fun updateWishCount(index: Int, count: Int) {
        if (count in _uiState.value.minTargetCount.._uiState.value.maxTargetCount) {
            _uiState.update { state ->
                val updatedWishes = state.wishes.mapIndexed { i, wish ->
                    if (i == index) wish.copy(targetCount = count) else wish
                }
                state.copy(wishes = updatedWishes)
            }
        }
    }
    
    private fun saveWish() {
        val currentState = _uiState.value
        if (!currentState.isSaveEnabled) {
            viewModelScope.launch {
                _effect.send(WishInputEffect.ShowValidationError(
                    ValidationField.WISH_TEXT,
                    "최소 하나의 위시를 입력해주세요"
                ))
            }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isSaving = true) }
            
            try {
                // Save the first valid wish (single wish mode for now)
                val validWish = currentState.wishes.firstOrNull { 
                    it.wishText.isNotBlank() && it.targetCount > 0 
                }
                
                if (validWish != null) {
                    // Save to repository
                    val today = LocalDate.now().toString()
                    val existing = wishRepository.getWishCountByDate(today)
                    
                    if (existing != null) {
                        // Update existing
                        wishRepository.saveWishCount(existing.copy(
                            wishText = validWish.wishText,
                            targetCount = validWish.targetCount
                        ))
                    } else {
                        // Create new
                        val newWish = com.wishring.app.data.model.WishUiState(
                            date = today,
                            wishText = validWish.wishText,
                            targetCount = validWish.targetCount,
                            currentCount = 0,
                            isCompleted = false,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        wishRepository.saveWishCount(newWish)
                    }
                    
                    val message = if (currentState.isEditMode) {
                        "위시가 수정되었습니다"
                    } else {
                        "위시가 등록되었습니다"
                    }
                    
                    _effect.send(WishInputEffect.ShowToast(message))
                    _effect.send(WishInputEffect.NavigateToHomeWithResult("위시 저장 완료"))
                }
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        error = "위시 저장에 실패했습니다: ${e.message}",
                        isSaving = false
                    )
                }
            } finally {
                _uiState.update { state -> state.copy(isSaving = false) }
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
                val existing = wishRepository.getWishCountByDate(today)
                if (existing != null) {
                    wishRepository.saveWishCount(existing.copy(targetCount = 0, wishText = ""))
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
    
    
    private fun resetToDefaults() {
        _uiState.update { state ->
            state.copy(
                wishes = listOf(WishDayUiState.empty(LocalDate.now())),
                isEditMode = false,
                existingRecord = false
            )
        }
    }
    
    private fun loadExistingRecord(date: String) {
        viewModelScope.launch {
            try {
                val recordDate = LocalDate.parse(date)
                val record = wishRepository.getWishDay(recordDate.toString())
                
                if (record != null && record.wishText.isNotBlank()) {
                    val existingWish = WishDayUiState(
                        date = recordDate,
                        wishText = record.wishText,
                        isCompleted = record.isCompleted,
                        targetCount = record.targetCount,
                        completedCount = record.completedCount
                    )
                    _uiState.update { state ->
                        state.copy(
                            wishes = listOf(existingWish),
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
        val currentState = _uiState.value
        val validWishCount = currentState.validWishCount
        
        if (validWishCount == 0) {
            viewModelScope.launch {
                _effect.send(WishInputEffect.ShowValidationError(
                    ValidationField.WISH_TEXT,
                    "최소 하나의 위시를 입력해주세요"
                ))
            }
        }
    }
    
    private fun clearWishText() {
        _uiState.update { state ->
            val updatedWishes = state.wishes.map { wish ->
                wish.copy(wishText = "")
            }
            state.copy(wishes = updatedWishes)
        }
    }
}