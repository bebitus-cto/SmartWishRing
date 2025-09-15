package com.wishring.app.ble

import android.content.Context
import android.util.Log
import com.manridy.sdk_mrd2019.Manridy
// import com.manridy.sdk_mrd2019.MrdReadCallBack
// import com.manridy.sdk_mrd2019.MrdReadEnum
import com.manridy.sdk_mrd2019.bean.send.SystemEnum
import com.manridy.sdk_mrd2019.read.MrdReadRequest
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MRD SDK Protocol Adapter
 * Handles battery and counter operations only
 */
@Singleton
class MrdProtocolAdapter @Inject constructor(
    private val context: Context
) {
    
    // Button press events stream
    private val _buttonPresses = MutableSharedFlow<Int>()
    
    // Command callback to BleRepositoryImpl
    private var onCommandReadyCallback: ((ByteArray) -> Unit)? = null
    
    // SDK initialization status
    private var isInitialized = false
    
    // 디버깅용 이벤트 히스토리
    data class EventHistoryItem(
        val timestamp: Long,
        val eventType: String,
        val jsonData: String,
        val description: String = ""
    )
    
    private val _eventHistory = mutableListOf<EventHistoryItem>()
    private val _eventHistoryFlow = MutableSharedFlow<List<EventHistoryItem>>()
    
    /**
     * 이벤트 히스토리 관찰용 플로우
     */
    fun getEventHistoryFlow(): Flow<List<EventHistoryItem>> = _eventHistoryFlow.asSharedFlow()
    
    /**
     * 현재 이벤트 히스토리 목록 가져오기
     */
    fun getEventHistory(): List<EventHistoryItem> = _eventHistory.toList()
    
    /**
     * 이벤트 히스토리 초기화
     */
    fun clearEventHistory() {
        _eventHistory.clear()
        _eventHistoryFlow.tryEmit(_eventHistory.toList())
    }
    
    /**
     * Initialize MRD SDK and setup callbacks
     */
    fun initialize(onCommandReady: (ByteArray) -> Unit) {
        this.onCommandReadyCallback = onCommandReady
        
        try {
            // MRD SDK should already be initialized in Application class
            isInitialized = true
            Log.d("MrdProtocolAdapter", "MRD Protocol Adapter initialized successfully")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize MRD SDK", e)
        }
    }
    
    /**
     * Handle data received from BLE device
     * Forward to SDK for parsing
     */
    fun onDataReceived(data: ByteArray) {
        if (!isInitialized) return
        
        try {
            // Use MRD SDK for data parsing
            val readRequest: MrdReadRequest = Manridy.getMrdRead().read(data)
            handleParsedData(readRequest)
        } catch (e: Exception) {
            Log.e("MrdProtocolAdapter", "Error parsing received data", e)
        }
    }
    
    /**
     * Request battery level from device
     */
    fun requestBatteryLevel() {
        if (!isInitialized) return
        
        try {
            val command = Manridy.getMrdSend().getSystem(SystemEnum.battery).getDatas()
            onCommandReadyCallback?.invoke(command)
        } catch (e: Exception) {
            Log.e("MrdProtocolAdapter", "Error requesting battery level", e)
        }
    }
    
    /**
     * Subscribe to button press events from the device
     * HEART events = button presses for wish counting
     */
    fun subscribeToButtonPresses(): Flow<Int> = _buttonPresses.asSharedFlow()
    
    
    /**
     * 이벤트 히스토리에 추가
     */
    private fun addToEventHistory(eventType: String, jsonData: String) {
        val item = EventHistoryItem(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            jsonData = jsonData,
            description = getEventDescription(eventType)
        )
        
        _eventHistory.add(item)
        
        // 최대 50개까지만 보관
        if (_eventHistory.size > 50) {
            _eventHistory.removeAt(0)
        }
        
        // 업데이트된 히스토리를 플로우로 전송
        _eventHistoryFlow.tryEmit(_eventHistory.toList())
        
        Log.d("MrdProtocolAdapter", "📝 이벤트 히스토리 추가: $eventType (총 ${_eventHistory.size}개)")
    }
    
    /**
     * 이벤트 타입별 설명
     */
    private fun getEventDescription(eventType: String): String {
        return when (eventType) {
            "HEART" -> "심박수 데이터 (기존 처리)"
            "Sport_realTime" -> "운동 실시간 데이터 (가능한 카운터)"
            "Battery" -> "배터리 정보"
            "Step_realTime" -> "걸음수 실시간 데이터"
            "AnswerPhone" -> "전화 받기"
            "Start_Or_Pause" -> "음악 재생/일시정지"
            else -> "기타 이벤트"
        }
    }

    /**
     * Handle parsed data from MRD SDK
     */
    private fun handleParsedData(readRequest: MrdReadRequest) {
        if (readRequest.status == 0) {
            Log.w("MrdProtocolAdapter", "Failed to parse data")
            return
        }
        
        val enumType = readRequest.mrdReadEnum
        val jsonData = readRequest.json
        
        // ✅ 모든 이벤트를 상세히 로그로 기록 (디버깅용)
        Log.d("MrdProtocolAdapter", "========================================")
        Log.d("MrdProtocolAdapter", "🔍 [이벤트 수신] 타입: ${enumType.name}")
        Log.d("MrdProtocolAdapter", "📊 [JSON 데이터]: $jsonData")
        Log.d("MrdProtocolAdapter", "⚡ [상태코드]: ${readRequest.status}")
        Log.d("MrdProtocolAdapter", "========================================")
        
        // 이벤트 히스토리에 추가
        addToEventHistory(enumType.name, jsonData)
        
        // Process based on the response type
        when (enumType.name) {
            "Battery" -> {
                Log.d("MrdProtocolAdapter", "🔋 배터리 데이터 처리 중")
            }
            "HEART" -> {
                Log.d("MrdProtocolAdapter", "💓 HEART 이벤트 감지 - 기존 처리 방식")
                _buttonPresses.tryEmit(1)
            }
            "Sport_realTime" -> {
                Log.d("MrdProtocolAdapter", "🏃 Sport_realTime 이벤트 감지 - 잠재적 카운터")
                // TODO: 운동 실시간 데이터에서 카운트 추출
                Log.d("MrdProtocolAdapter", "Sport_realTime 데이터: $jsonData")
                _buttonPresses.tryEmit(1) // 임시로 1 전송
            }
            else -> {
                Log.d("MrdProtocolAdapter", "❓ 기타 데이터 타입: ${enumType.name}")
            }
        }
    }
}