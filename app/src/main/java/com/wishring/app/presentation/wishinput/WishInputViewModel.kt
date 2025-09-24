package com.wishring.app.presentation.wishinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.repository.WishRepository
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.presentation.wishinput.model.WishItem
import com.wishring.app.presentation.wishinput.model.toWishDataList
import com.wishring.app.presentation.wishinput.model.toWishItemList
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
            is WishInputEvent.RemoveWish -> removeWish(event.wishId)
            is WishInputEvent.UpdateWishText -> updateWishText(event.wishId, event.text)
            is WishInputEvent.UpdateWishCount -> updateWishCount(event.wishId, event.count)
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
            try {
                val today = LocalDate.now()
                val todayWishes = wishRepository.getTodayWishes()
                val todayWishCount = wishRepository.getTodayWish()
                
                if (todayWishes.isNotEmpty() && todayWishCount != null) {
                    // Convert existing wishes to WishItem list with shared target count
                    val existingWishes = todayWishes.toWishItemList(todayWishCount.currentCount)
                    _uiState.update { state ->
                        state.copy(
                            wishes = existingWishes,
                            isEditMode = true,
                            existingRecord = true
                        )
                    }
                } else {
                    // Fallback to old single wish approach for backward compatibility
                    val record = wishRepository.getDailyRecord(today.toString())
                    if (record != null && record.wishText.isNotBlank()) {
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
            } catch (e: Exception) {
                // Handle error silently, keep default state
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
                // Save all valid wishes as JSON in the database
                val validWishes = currentState.wishes.filter { it.isValid }
                
                if (validWishes.isNotEmpty()) {
                    // Convert WishItems to WishData format for storage
                    val wishDataList = validWishes.toWishDataList()
                    
                    // Update repository with multiple wishes
                    // All wishes share the same target count from the first valid wish
                    val targetCount = validWishes.first().targetCount
                    wishRepository.updateTodayWishesAndTarget(
                        wishesData = wishDataList,
                        targetCount = targetCount,
                        activeWishIndex = 0 // Default to first wish as active
                    )
                    
                    // Handle date change edge case
                    val currentDate = LocalDate.now()
                    val wishCreationDate = validWishes.first().creationDate
                    
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
                
                // Try to get wishes for the specific date
                // Note: This would need a new repository method getTodayWishes(date: String)
                // For now, we'll use the existing getDailyRecord as fallback
                val record = wishRepository.getDailyRecord(recordDate.toString())
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
    
    private fun clearWishText() {
        _uiState.update { state ->
            val updatedWishes = state.wishes.map { wish ->
                wish.copy(text = "")
            }
            state.copy(wishes = updatedWishes)
        }
    }
}