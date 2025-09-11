package com.wishring.app.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * Base ViewModel implementing UDA (Unidirectional Data Architecture) pattern
 * Manages State, Events, and Effects in a consistent way
 * 
 * @param State UI state type
 * @param Event User event type
 * @param Effect Side effect type (navigation, toast, etc.)
 */
abstract class BaseViewModel<State, Event, Effect> : ViewModel() {
    
    /**
     * Mutable state flow - internal use only
     */
    protected abstract val _uiState: MutableStateFlow<State>
    
    /**
     * Public state flow for UI observation
     */
    val uiState: StateFlow<State> 
        get() = _uiState.asStateFlow()
    
    /**
     * Current state value
     */
    val currentState: State
        get() = _uiState.value
    
    /**
     * Effect channel for one-time events
     */
    private val _effect = Channel<Effect>(Channel.BUFFERED)
    
    /**
     * Public effect flow for UI observation
     */
    val effect = _effect.receiveAsFlow()
    
    /**
     * Handle incoming events from UI
     * @param event User event to process
     */
    abstract fun onEvent(event: Event)
    
    /**
     * Update state using a lambda
     * @param update Lambda to modify current state
     */
    protected inline fun updateState(update: State.() -> State) {
        _uiState.value = _uiState.value.update()
    }
    
    /**
     * Send an effect to the UI
     * @param effect Effect to send
     */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
    
    /**
     * Execute a suspending block with loading state management
     * Requires State to have isLoading property
     * @param block Suspending block to execute
     * @return Result of the block
     */
    protected suspend fun <T> withLoading(
        block: suspend () -> T
    ): T {
        updateState { 
            @Suppress("UNCHECKED_CAST")
            when (this) {
                is LoadingState -> setLoading(true) as State
                else -> this
            }
        }
        
        return try {
            block()
        } finally {
            updateState { 
                @Suppress("UNCHECKED_CAST")
                when (this) {
                    is LoadingState -> setLoading(false) as State
                    else -> this
                }
            }
        }
    }
    
    /**
     * Handle errors consistently
     * @param throwable Error to handle
     * @param fallback Optional fallback action
     */
    protected open fun handleError(
        throwable: Throwable,
        fallback: (() -> Unit)? = null
    ) {
        Log.e("BaseViewModel", "Exception occurred", throwable)
        fallback?.invoke()
        
        // Update state with error if it supports ErrorState
        updateState {
            @Suppress("UNCHECKED_CAST")
            when (this) {
                is ErrorState -> setError(throwable.message) as State
                else -> this
            }
        }
    }
    
    /**
     * Launch a coroutine in viewModelScope with error handling
     * @param block Suspending block to execute
     */
    protected fun launch(
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
    
    /**
     * Launch a coroutine with loading state
     * @param block Suspending block to execute
     */
    protected fun launchWithLoading(
        block: suspend () -> Unit
    ) {
        viewModelScope.launch {
            try {
                withLoading {
                    block()
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }
}

/**
 * Interface for states that support loading indicator
 */
interface LoadingState {
    val isLoading: Boolean
    fun setLoading(loading: Boolean): LoadingState
}

/**
 * Interface for states that support error messages
 */
interface ErrorState {
    val error: String?
    fun setError(error: String?): ErrorState
}