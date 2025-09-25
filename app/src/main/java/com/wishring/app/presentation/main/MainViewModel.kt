package com.wishring.app.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wishring.app.data.model.ConnectedDevice
import com.wishring.app.data.repository.BleConnectionState
import com.wishring.app.data.repository.PreferencesRepository
import com.wishring.app.data.repository.WishRepository
import com.wishring.app.data.model.WishDayUiState
import com.wishring.app.data.model.WishUiState
import com.wishring.app.data.local.database.entity.WishData
import com.wishring.app.presentation.home.PageInfo
import com.wishring.app.data.ble.model.BleConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val wishRepository: WishRepository
) : ViewModel() {


    // BLE 통합 상태 관리 (위시 데이터 포함)
    private val _bleCommand = MutableStateFlow(BleCommand())
    val bleCommand = _bleCommand.asStateFlow()
    
    // 위시 데이터 초기 로딩 플래그
    private var isInitialDataLoaded = false
    
    init {
        // seedTestData() // 주석 처리됨
        if (!isInitialDataLoaded) {
            loadInitialWishData()
        }
        observeTodayWishCount()
    }

    fun updateBatteryLevel(batteryLevel: Int) {
        val currentState = _bleCommand.value
        Log.d(WR_EVENT, "[배터리] MainViewModel - 배터리 레벨 업데이트 요청: $batteryLevel%")
        Log.d(WR_EVENT, "[배터리] MainViewModel - 현재 연결 상태: ${currentState.connectionState}")
        Log.d(WR_EVENT, "[배터리] MainViewModel - 현재 BLE Phase: ${currentState.phase}")
        Log.d(WR_EVENT, "[배터리] MainViewModel - 기존 배터리 레벨: ${currentState.batteryLevel}%")
        
        _bleCommand.value = _bleCommand.value.copy(
            batteryLevel = batteryLevel,
        )
        
        Log.d(WR_EVENT, "[배터리] MainViewModel - 업데이트 완료: ${_bleCommand.value.batteryLevel}%")
        Log.i(WR_EVENT, "[MainViewModel] 배터리 업데이트 - 레벨: ${batteryLevel}%")
    }

    fun updateConnectionState(connected: Boolean) {
        val currentState = _bleCommand.value

        _bleCommand.value = currentState.copy(
            connectionState = if (connected) BleConnectionState.CONNECTED
            else BleConnectionState.DISCONNECTED,
            phase = if (connected) BlePhase.Idle else currentState.phase,
            scannedDevices = if (connected) emptyList() else currentState.scannedDevices
        )

        Log.i(WR_EVENT, "[MainViewModel] 연결 상태 변경: $connected")

        if (connected) {
            Log.i(WR_EVENT, "[MainViewModel] 연결 성공 - 기기 목록 및 스캔 상태 정리")
        }
    }

    fun addScannedDevice(
        name: String,
        address: String,
        rssi: Int,
        serviceUuids: List<String> = emptyList()
    ) {

        if (validateWishRingDevice(serviceUuids)) {
            Log.i(WR_EVENT, "[MainViewModel] ✅ WISH RING 기기 확인됨 (Service UUID 검증)")

            val currentState = _bleCommand.value
            val devices = currentState.scannedDevices.toMutableList()

            // 중복 제거
            devices.removeAll { it.address == address }
            devices.add(DeviceInfo(name, address, rssi))

            // RSSI 강한 순으로 정렬
            val sortedDevices = devices.sortedByDescending { it.rssi }

            // BleUiState 업데이트
            _bleCommand.value = currentState.copy(
                scannedDevices = sortedDevices
            )

            Log.i(WR_EVENT, "[MainViewModel] 현재 WISH RING 기기 목록 크기: ${sortedDevices.size}")
            Log.i(
                WR_EVENT,
                "[MainViewModel] shouldShowDevicePicker 조건: devices=${sortedDevices.isNotEmpty()}, phase=${currentState.phase == BlePhase.Idle}"
            )
            Log.i(
                WR_EVENT,
                "[MainViewModel] → shouldShowDevicePicker = ${sortedDevices.isNotEmpty() && currentState.phase == BlePhase.Idle}"
            )
        } else {
            // 무시 - 너무 많은 로그 생성
        }

        Log.i(WR_EVENT, "[MainViewModel] ================================================")
    }

    private fun validateWishRingDevice(serviceUuids: List<String>): Boolean {
        // Service UUID로 H13 기기 식별
        val hasValidServiceUuid = serviceUuids.any { uuid ->
            uuid.equals(BleConstants.SERVICE_UUID.toString(), ignoreCase = true)
        }

        Log.i(WR_EVENT, "[MainViewModel] Service UUID 검증:")
        Log.i(WR_EVENT, "[MainViewModel] - 찾는 UUID: ${BleConstants.SERVICE_UUID}")
        Log.i(WR_EVENT, "[MainViewModel] - 기기의 UUIDs: $serviceUuids")
        Log.i(WR_EVENT, "[MainViewModel] - 검증 결과: ${if (hasValidServiceUuid) "✅ 일치" else "❌ 불일치"}")

        return hasValidServiceUuid
    }

    fun clearScannedDevices() {
        _bleCommand.value = _bleCommand.value.copy(
            scannedDevices = emptyList(),
            phase = BlePhase.Idle
        )
        Log.i(WR_EVENT, "[MainViewModel] 기기 목록 초기화")
    }

    fun dismissDevicePicker() {
        _bleCommand.value = _bleCommand.value.copy(
            scannedDevices = emptyList(),
            phase = BlePhase.Idle
        )
        Log.i(WR_EVENT, "[MainViewModel] 기기 선택 다이얼로그 닫기")
    }
    
    /**
     * 기기가 선택되었음을 표시 (Dialog는 닫지만 연결 준비중 상태 유지)
     */
    fun selectDevice() {
        _bleCommand.value = _bleCommand.value.copy(
            scannedDevices = emptyList(),
            phase = BlePhase.DeviceSelected
        )
        Log.i(WR_EVENT, "[MainViewModel] 기기 선택됨 - 연결 준비중")
    }

    // ===== 자동 연결 기능 =====

    /**
     * 연결 성공 시 기기 정보 저장
     */
    suspend fun onDeviceConnected(deviceAddress: String, deviceName: String) {
        Log.i(WR_EVENT, "[MainViewModel] 기기 연결 성공: $deviceName ($deviceAddress)")

        try {
            // 기존 기기 정보가 있으면 업데이트, 없으면 새로 생성
            val existingDevice = preferencesRepository.getLastConnectedDevice()
            val device = if (existingDevice?.address == deviceAddress) {
                existingDevice.copy(
                    name = deviceName, // 이름 업데이트
                    lastConnectedTime = System.currentTimeMillis(),
                    connectionCount = existingDevice.connectionCount + 1
                )
            } else {
                ConnectedDevice.create(deviceAddress, deviceName)
            }

            preferencesRepository.saveConnectedDevice(device)
            Log.i(WR_EVENT, "[MainViewModel] 기기 정보 저장 완료")

        } catch (e: Exception) {
            Log.e(WR_EVENT, "[MainViewModel] 기기 정보 저장 실패", e)
        }
    }

    // ===== BLE 스캔 관련 메서드 =====

    fun actuallyStartBleScan() {
        // 스캔 상태로 변경
        _bleCommand.value = _bleCommand.value.copy(
            phase = BlePhase.Scanning,
            scannedDevices = emptyList() // 새 스캔 시작 시 기존 목록 클리어
        )

        Log.i(WR_EVENT, "[MainViewModel] BLE 스캔 상태로 변경됨")
    }

    fun stopBleScan() {
        Log.i(WR_EVENT, "[MainViewModel] BLE 스캔 중지 요청")
        // 스캔 중지 시 Idle 상태로 변경
        _bleCommand.value = _bleCommand.value.copy(phase = BlePhase.Idle)
        // MainActivity가 이 상태를 관찰하여 Discovery를 중지함
    }

    fun onDiscoveryFinished() {
        val currentState = _bleCommand.value
        val hasDevices = currentState.scannedDevices.isNotEmpty()

        Log.i(WR_EVENT, "[MainViewModel] Discovery 완료됨")
        Log.i(WR_EVENT, "[MainViewModel] - 발견된 WISH RING 기기 수: ${currentState.scannedDevices.size}")

        // Discovery 완료 시 Idle 상태로 변경
        _bleCommand.value = currentState.copy(phase = BlePhase.Idle)

        // 기기가 발견되었으면 다이얼로그 표시 조건 확인
        if (hasDevices) {
            Log.i(WR_EVENT, "[MainViewModel] ✅ 기기 발견됨 - 다이얼로그 표시 가능")
            Log.i(
                WR_EVENT,
                "[MainViewModel] shouldShowDevicePicker = ${_bleCommand.value.shouldShowDevicePicker}"
            )

            // 디버그: 다이얼로그가 표시되어야 하는데 표시되지 않는 경우
            if (!_bleCommand.value.shouldShowDevicePicker) {
                Log.e(WR_EVENT, "[MainViewModel] ⚠️ 다이얼로그가 표시되어야 하는데 표시되지 않음!")
                Log.e(WR_EVENT, "[MainViewModel] - phase: ${_bleCommand.value.phase}")
                Log.e(
                    WR_EVENT,
                    "[MainViewModel] - scannedDevices: ${_bleCommand.value.scannedDevices}"
                )
            }
        } else {
            Log.i(WR_EVENT, "[MainViewModel] ❌ 발견된 기기 없음")
        }
    }


    /**
     * BLE 스캔 시작 (외부 호출용)
     */
    fun startBleScan() {
        actuallyStartBleScan()
    }

    // ===== 위시 데이터 관리 메서드들 =====
    
    fun loadInitialWishData() {
        viewModelScope.launch {
            Log.d(WR_EVENT, "[WishHistory] === loadInitialWishData 시작 ===")
            
            // Start with loading state
            _bleCommand.update { currentState ->
                currentState.copy(isWishDataLoading = true, wishDataError = null)
            }

            try {
                // Load first page (50 items)
                Log.d(WR_EVENT, "[WishHistory] getWishHistoryPaginated 호출 - page: 0")
                val (wishHistory, pageInfo) = getWishHistoryPaginated(page = 0)
                Log.d(WR_EVENT, "[WishHistory] 로드 완료 - wishHistory 크기: ${wishHistory.size}")
                
                wishHistory.forEachIndexed { index, record ->
                    Log.d(WR_EVENT, "[WishHistory] [$index] ${record.date} - ${record.wishText} (count: ${record.completedCount}/${record.targetCount})")
                }
                
                val todayWish = getTodayWishFromHistory(wishHistory)
                Log.d(WR_EVENT, "[WishHistory] todayWish: ${todayWish?.let { "${it.currentCount}/${it.targetCount}" } ?: "null"}")

                updateWishData(wishHistory, todayWish, pageInfo, null)
                isInitialDataLoaded = true
                Log.d(WR_EVENT, "[WishHistory] === loadInitialWishData 완료 ===")

            } catch (e: Exception) {
                Log.e(WR_EVENT, "[WishHistory] loadInitialWishData 오류", e)
                val errorMessage = e.message ?: "데이터를 불러오는 중 오류가 발생했습니다"
                _bleCommand.update { currentState ->
                    currentState.copy(isWishDataLoading = false, wishDataError = errorMessage)
                }
            }
        }
    }

    fun loadMoreWishes() {
        viewModelScope.launch {
            Log.d(WR_EVENT, "[WishHistory] === loadMoreWishes 시작 ===")
            
            val currentPageInfo = _bleCommand.value.pageInfo
            if (currentPageInfo == null || !currentPageInfo.hasNextPage || _bleCommand.value.isWishDataLoading) {
                Log.d(WR_EVENT, "[WishHistory] loadMoreWishes 중단 - pageInfo: $currentPageInfo, isLoading: ${_bleCommand.value.isWishDataLoading}")
                return@launch
            }

            // Set loading state
            _bleCommand.update { currentState ->
                currentState.copy(isWishDataLoading = true)
            }

            val nextPage = currentPageInfo.currentPage + 1
            Log.d(WR_EVENT, "[WishHistory] getWishHistoryPaginated 호출 - page: $nextPage")

            try {
                val (newWishHistory, newPageInfo) = getWishHistoryPaginated(page = nextPage)
                Log.d(WR_EVENT, "[WishHistory] 추가 로드 완료 - newWishHistory 크기: ${newWishHistory.size}")
                
                val combinedHistory = _bleCommand.value.wishHistory + newWishHistory
                Log.d(WR_EVENT, "[WishHistory] 전체 크기: ${_bleCommand.value.wishHistory.size} + ${newWishHistory.size} = ${combinedHistory.size}")
                
                newWishHistory.forEachIndexed { index, record ->
                    Log.d(WR_EVENT, "[WishHistory] [추가$index] ${record.date} - ${record.wishText} (count: ${record.completedCount}/${record.targetCount})")
                }
                
                val todayWish = getTodayWishFromHistory(combinedHistory)
                Log.d(WR_EVENT, "[WishHistory] 재계산된 todayWish: ${todayWish?.let { "${it.currentCount}/${it.targetCount}" } ?: "null"}")

                updateWishData(combinedHistory, todayWish, newPageInfo, _bleCommand.value.batteryLevel)
                
                Log.d(WR_EVENT, "[WishHistory] === loadMoreWishes 완료 ===")

            } catch (e: Exception) {
                Log.e(WR_EVENT, "[WishHistory] loadMoreWishes 오류", e)
                val errorMessage = e.message ?: "추가 데이터를 불러오는 중 오류가 발생했습니다"
                _bleCommand.update { currentState ->
                    currentState.copy(isWishDataLoading = false, wishDataError = errorMessage)
                }
            }
        }
    }

    private suspend fun getWishHistoryPaginated(page: Int): Pair<List<WishDayUiState>, PageInfo> {
        return wishRepository.getWishHistoryPaginated(page = page, pageSize = 50) // 100 → 50 변경
    }

    private fun getTodayWishFromHistory(wishHistory: List<WishDayUiState>): WishUiState? {
        // 첫 번째 항목이 실제 오늘 날짜인 경우에만 todayWish로 반환
        return wishHistory.firstOrNull()?.let { wishDay ->
            if (wishDay.date == LocalDate.now()) {
                WishUiState.fromWishDay(wishDay)
            } else {
                null
            }
        }
    }

    private fun updateWishData(
        wishHistory: List<WishDayUiState>,
        todayWish: WishUiState?,
        pageInfo: PageInfo?,
        batteryLevel: Int?
    ) {
        val displayWishHistory = if (todayWish != null) {
            wishHistory.drop(1)
        } else {
            wishHistory
        }
        
        _bleCommand.update { currentState ->
            currentState.copy(
                wishHistory = displayWishHistory,
                todayWish = todayWish,
                pageInfo = pageInfo,
                isWishDataLoading = false,
                wishDataError = null,
                batteryLevel = batteryLevel ?: currentState.batteryLevel
            )
        }
    }

    private fun observeTodayWishCount() {
        wishRepository.observeTodayWishCount()
            .onEach { wishCount ->
                _bleCommand.update { currentState ->
                    currentState.copy(todayWish = wishCount)
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Dismiss wish data error
     */
    fun dismissWishDataError() {
        _bleCommand.update { currentState ->
            currentState.copy(wishDataError = null)
        }
    }

    // ===== 위시 개수 기반 상태 결정 로직 =====
    
    /**
     * 오늘의 등록된 위시 목록을 가져옴
     * @return 오늘 등록된 위시들의 리스트
     */
    suspend fun getTodayWishes(): List<WishData> {
        return try {
            wishRepository.getTodayWishes()
        } catch (e: Exception) {
            Log.e(WR_EVENT, "[WishCount] getTodayWishes 오류", e)
            emptyList()
        }
    }
    
    /**
     * 등록된 위시 개수를 기반으로 상태를 결정
     * @return 위시 개수에 따른 상태 결정
     */
    /**
     * 등록된 위시 개수를 기반으로 상태를 결정하고 BleCommand 업데이트
     * HomeViewState 로직을 BleCommand로 통합
     */
    fun determineStateByWishCount() {
        viewModelScope.launch {
            try {
                val todayWishes = getTodayWishes()
                val wishCount = todayWishes.size
                
                Log.d(WR_EVENT, "[WishCount] 등록된 위시 개수: $wishCount")
                todayWishes.forEachIndexed { index, wish ->
                    Log.d(WR_EVENT, "[WishCount] [$index] ${wish.text}")
                }
                
                val currentState = _bleCommand.value
                val homeViewStateType = currentState.getHomeViewStateType(wishCount)
                
                Log.d(WR_EVENT, "[WishCount] 현재 HomeViewState 타입: $homeViewStateType")
                Log.d(WR_EVENT, "[WishCount] - 연결 상태: ${currentState.isConnected}")
                Log.d(WR_EVENT, "[WishCount] - 위시 등록 프롬프트 표시: ${currentState.showWishRegistrationPrompt}")
                Log.d(WR_EVENT, "[WishCount] - 위시 버튼 표시: ${currentState.showWishButton}")
                Log.d(WR_EVENT, "[WishCount] - 완료 애니메이션: ${currentState.showCompletionAnimation}")
                
                // 실제 위시 개수 정보로 상태 결정 (BleCommand의 computed property 활용)
                val stateBasedOnWishCount = when {
                    wishCount == 0 -> {
                        Log.d(WR_EVENT, "[WishCount] → 위시 등록 필요 상태")
                        "NO_WISHES_REGISTERED"
                    }
                    wishCount == 1 -> {
                        Log.d(WR_EVENT, "[WishCount] → 단일 위시 모드")
                        "SINGLE_WISH_MODE"
                    }
                    wishCount in 2..2 -> {
                        Log.d(WR_EVENT, "[WishCount] → 부분 위시 모드 (2개)")
                        "PARTIAL_WISHES_MODE"
                    }
                    wishCount >= 3 -> {
                        Log.d(WR_EVENT, "[WishCount] → 완전 위시 모드 (${wishCount}개)")
                        "FULL_WISHES_MODE"
                    }
                    else -> {
                        Log.d(WR_EVENT, "[WishCount] → 기본 상태")
                        "DEFAULT_STATE"
                    }
                }
                
                Log.i(WR_EVENT, "[MainViewModel] 위시 개수 기반 상태 결정 완료 - $stateBasedOnWishCount")
                
            } catch (e: Exception) {
                Log.e(WR_EVENT, "[WishCount] determineStateByWishCount 오류", e)
            }
        }
    }

    companion object {
        const val WR_EVENT = "WR_EVENT"
    }
}