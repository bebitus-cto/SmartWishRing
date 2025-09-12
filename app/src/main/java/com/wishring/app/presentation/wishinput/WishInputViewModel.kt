package com.wishring.app.presentation.wishinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.domain.model.WishCount
import com.wishring.app.domain.repository.WishCountRepository
import com.wishring.app.domain.repository.PreferencesRepository
import com.wishring.app.presentation.wishinput.model.WishItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
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
     * Handle user events (supports multiple wishes)
     */
    fun onEvent(event: WishInputEvent) {
        when (event) {
            is WishInputEvent.AddWish -> addWish(event.position)
            is WishInputEvent.RemoveWish -> removeWish(event.wishId)
            is WishInputEvent.UpdateWishText -> updateWishText(event.wishId, event.text)
            is WishInputEvent.UpdateWishCount -> updateWishCount(event.wishId, event.count)
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
                
                // Create initial wish item with defaults
                val initialWish = WishItem.create(defaultWish, defaultTarget)
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
            val today = LocalDate.now()
            val record = wishCountRepository.getDailyRecord(today.toString())
            if (record != null && record.wishText.isNotBlank()) {
                // Convert existing record to wish item
                val existingWish = WishItem.create(record.wishText, record.targetCount)
                _uiState.update { state ->
                    state.copy(
                        wishes = listOf(existingWish),
                        isEditMode = true,
                        existingRecord = true
                    )
                }
            }
        }
    }
    
    private fun addWish(position: Int? = null) {
        val currentState = _uiState.value
        if (currentState.canAddMoreWishes) {
            val newWish = WishItem.createEmpty()
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
    
    private fun removeWish(wishId: UUID) {
        val currentState = _uiState.value
        if (currentState.canRemoveWishes) {
            val updatedWishes = currentState.wishes.filterNot { it.id == wishId }
            _uiState.update { state ->
                state.copy(wishes = updatedWishes)
            }
        }
    }
    
    private fun updateWishText(wishId: UUID, text: String) {
        if (text.length <= _uiState.value.maxWishLength) {
            _uiState.update { state ->
                val updatedWishes = state.wishes.map { wish ->
                    if (wish.id == wishId) wish.copy(text = text) else wish
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
    
    private fun updateWishCount(wishId: UUID, count: Int) {
        if (count in _uiState.value.minTargetCount.._uiState.value.maxTargetCount) {
            _uiState.update { state ->
                val updatedWishes = state.wishes.map { wish ->
                    if (wish.id == wishId) wish.copy(targetCount = count) else wish
                }
                state.copy(wishes = updatedWishes)
            }
        }
    }
    
    private fun selectSuggestedWish(wish: String) {
        _uiState.update { state ->
            // Apply suggested wish to the first empty wish item, or add a new one
            val updatedWishes = if (state.wishes.any { it.text.isBlank() }) {
                state.wishes.map { wishItem ->
                    if (wishItem.text.isBlank()) {
                        wishItem.copy(text = wish)
                    } else {
                        wishItem
                    }
                }
            } else if (state.canAddMoreWishes) {
                state.wishes + WishItem.create(wish, 1000)
            } else {
                state.wishes
            }
            
            state.copy(
                wishes = updatedWishes,
                showSuggestions = false
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
                currentValue = 1000, // Default value since we handle multiple wishes
                minValue = _uiState.value.minTargetCount,
                maxValue = _uiState.value.maxTargetCount,
                onValueSelected = { count ->
                    // This would need to be updated when we know which wish to update
                    // For now, we'll handle target count changes directly in the component
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
                // Save only valid wishes (combine multiple wishes into one text)
                val validWishes = currentState.wishes.filter { it.isValid }
                
                if (validWishes.isNotEmpty()) {
                    // For now, save the first valid wish to maintain compatibility
                    // In future versions, this could be enhanced to save multiple wishes
                    val firstWish = validWishes.first()
                    
                    wishCountRepository.updateTodayWishAndTarget(
                        wishText = firstWish.text,
                        targetCount = firstWish.targetCount
                    )
                    
                    // Handle 12시 넘김 edge case
                    val currentDate = LocalDate.now()
                    val wishCreationDate = firstWish.creationDate
                    
                    if (currentDate != wishCreationDate) {
                        _effect.send(WishInputEffect.ShowToast(
                            "날짜가 변경되었습니다. ${wishCreationDate} 위시로 저장됩니다."
                        ))
                    }
                    
                    val message = when {
                        currentState.isEditMode -> "위시가 수정되었습니다"
                        validWishes.size > 1 -> "${validWishes.size}개의 위시가 등록되었습니다"
                        else -> "위시가 등록되었습니다"
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
            val clearedWishes = state.wishes.map { wish ->
                wish.copy(text = "")
            }
            state.copy(wishes = clearedWishes)
        }
    }
    
    private fun resetToDefaults() {
        _uiState.update { state ->
            state.copy(
                wishes = listOf(WishItem.createEmpty()),
                isEditMode = false,
                existingRecord = false
            )
        }
    }
    
    private fun loadExistingRecord(date: String) {
        viewModelScope.launch {
            try {
                val recordDate = LocalDate.parse(date)
                val record = wishCountRepository.getDailyRecord(recordDate.toString())
                if (record != null && record.wishText.isNotBlank()) {
                    val existingWish = WishItem.create(record.wishText, record.targetCount)
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
}