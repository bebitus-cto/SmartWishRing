package com.wishring.app.ble

import android.bluetooth.*
import android.os.ParcelUuid
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.wishring.app.di.IoDispatcher
import com.wishring.app.domain.repository.BleConnectionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE 연결 관리자 클래스
 * 스마트 스캔, 자동 연결, 재연결 로직을 통합 관리
 */
@Singleton
class BleConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceValidator: WishRingDeviceValidator,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    companion object {
        private const val TAG = "BleConnectionManager"
    }
    
    // Bluetooth 관련 객체
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    // 연결 상태 관리
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()
    
    // 스캔 관련
    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()
    
    private var isScanning = false
    private var scanCallback: ScanCallback? = null
    
    // 연결 관련
    private var currentGatt: BluetoothGatt? = null
    private var gattCallback: BluetoothGattCallback? = null
    
    // 재연결 관련
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var lastConnectedDeviceAddress: String? = null
    
    // 배터리 모니터링
    private var batteryMonitoringJob: Job? = null
    
    // Health Check
    private var healthCheckJob: Job? = null
    
    // 코루틴 스코프
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    
    /**
     * 발견된 기기 정보
     */
    data class DiscoveredDevice(
        val device: BluetoothDevice,
        val rssi: Int,
        val serviceUuids: List<String>?,
        val discoveryTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 스마트 스캔 시작
     * 적응형 스캔 주기와 필터링 적용
     */
    fun startSmartScan() {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            _connectionState.value = BleConnectionState.ERROR
            return
        }
        
        if (isScanning) {
            Log.w(TAG, "⚠️ 스캔이 이미 진행중입니다! 기존 스캔을 중지하고 새로 시작합니다.")
            return
        }
        
        Log.d(TAG, "🔍 === BLE Smart Scan 시작 ===")
        Log.d(TAG, "📡 Service UUID: ${BleConstants.SERVICE_UUID}")
        Log.d(TAG, "🔄 현재 스캔 상태: isScanning=$isScanning")
        
        val scanFilters = createScanFilters()
        val scanSettings = createScanSettings()
        
        scanCallback = createScanCallback()
        
        try {
            bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
            isScanning = true
            
            // 스캔 타임아웃 설정
            Handler(Looper.getMainLooper()).postDelayed({
                stopScanning()
            }, BleConstants.SCAN_TIMEOUT_MS)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for BLE scan", e)
            _connectionState.value = BleConnectionState.ERROR
        }
    }
    
    /**
     * 스캔 중지
     */
    fun stopScanning() {
        if (!isScanning) return
        
        Log.d(TAG, "Stopping scan")
        
        try {
            scanCallback?.let { callback ->
                bluetoothLeScanner?.stopScan(callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for stopping scan", e)
        }
        
        isScanning = false
        scanCallback = null
    }
    
    /**
     * 기기 연결 시도
     */
    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(ioDispatcher) {
        if (_connectionState.value == BleConnectionState.CONNECTING || 
            _connectionState.value == BleConnectionState.CONNECTED) {
            Log.w(TAG, "Already connecting or connected")
            return@withContext false
        }
        
        Log.d(TAG, "Attempting to connect to ${device.address}")
        
        _connectionState.value = BleConnectionState.CONNECTING
        stopScanning()
        
        try {
            gattCallback = createGattCallback(device)
            currentGatt = device.connectGatt(context, false, gattCallback)
            
            // 연결 타임아웃 설정
            val connected = withTimeoutOrNull(BleConstants.CONNECTION_TIMEOUT_MS) {
                // connectionState가 CONNECTED가 될 때까지 대기
                connectionState.first { it == BleConnectionState.CONNECTED || it == BleConnectionState.ERROR }
                connectionState.value == BleConnectionState.CONNECTED
            }
            
            if (connected == true) {
                Log.d(TAG, "Successfully connected to ${device.address}")
                lastConnectedDeviceAddress = device.address
                reconnectAttempts = 0
                startBatteryMonitoring()
                startHealthCheck()
                return@withContext true
            } else {
                Log.w(TAG, "Connection timeout or failed")
                cleanup()
                return@withContext false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            cleanup()
            return@withContext false
        }
    }
    
    /**
     * 연결 해제
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from device")
        
        _connectionState.value = BleConnectionState.DISCONNECTING
        stopBatteryMonitoring()
        stopHealthCheck()
        cleanup()
    }
    
    /**
     * 자동 재연결 시작
     */
    fun startAutoReconnect() {
        val targetAddress = lastConnectedDeviceAddress
        if (targetAddress.isNullOrBlank()) {
            Log.w(TAG, "No previous device to reconnect to")
            return
        }
        
        if (reconnectJob?.isActive == true) {
            Log.d(TAG, "Auto reconnect already in progress")
            return
        }
        
        Log.d(TAG, "Starting auto reconnect to $targetAddress")
        
        reconnectJob = scope.launch {
            while (isActive && reconnectAttempts < BleConstants.MAX_RECONNECT_ATTEMPTS) {
                try {
                    val delay = BleConstants.calculateReconnectDelay(reconnectAttempts)
                    Log.d(TAG, "Reconnect attempt ${reconnectAttempts + 1} after ${delay}ms")
                    
                    delay(delay)
                    
                    if (_connectionState.value == BleConnectionState.CONNECTED) {
                        Log.d(TAG, "Already connected, stopping reconnect")
                        break
                    }
                    
                    val device = bluetoothAdapter?.getRemoteDevice(targetAddress)
                    if (device != null) {
                        val connected = connectToDevice(device)
                        if (connected) {
                            Log.d(TAG, "Auto reconnect successful")
                            break
                        }
                    }
                    
                    reconnectAttempts++
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Auto reconnect failed", e)
                    reconnectAttempts++
                }
            }
            
            if (reconnectAttempts >= BleConstants.MAX_RECONNECT_ATTEMPTS) {
                Log.w(TAG, "Max reconnect attempts reached, giving up")
            }
        }
    }
    
    /**
     * 배터리 모니터링 시작
     */
    private fun startBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        
        batteryMonitoringJob = scope.launch {
            while (isActive && _connectionState.value == BleConnectionState.CONNECTED) {
                try {
                    // 실제 배터리 정보 요청 (MrdProtocolAdapter 통해)
                    val batteryLevel = getCurrentBatteryLevel()
                    _batteryLevel.value = batteryLevel
                    
                    Log.d(TAG, "Battery level updated: $batteryLevel%")
                    
                    delay(BleConstants.BATTERY_MONITOR_INTERVAL_MS)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Battery monitoring failed", e)
                    delay(BleConstants.BATTERY_MONITOR_INTERVAL_MS)
                }
            }
        }
    }
    
    /**
     * 배터리 모니터링 중지
     */
    private fun stopBatteryMonitoring() {
        batteryMonitoringJob?.cancel()
        batteryMonitoringJob = null
    }
    
    /**
     * Health Check 시작 - 연결 상태 주기적 확인
     */
    private fun startHealthCheck() {
        healthCheckJob?.cancel()
        
        healthCheckJob = scope.launch {
            while (isActive && _connectionState.value == BleConnectionState.CONNECTED) {
                try {
                    delay(BleConstants.HEALTH_CHECK_INTERVAL_MS)
                    
                    // 간단한 Health Check (배터리 정보 요청)
                    val isHealthy = checkConnectionHealth()
                    
                    if (!isHealthy) {
                        Log.w(TAG, "Health check failed, attempting reconnect")
                        _connectionState.value = BleConnectionState.DISCONNECTED
                        startAutoReconnect()
                        break
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Health check failed", e)
                }
            }
        }
    }
    
    /**
     * Health Check 중지
     */
    private fun stopHealthCheck() {
        healthCheckJob?.cancel()
        healthCheckJob = null
    }
    
    /**
     * 연결 정리
     */
    private fun cleanup() {
        reconnectJob?.cancel()
        
        try {
            currentGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing GATT", e)
        }
        
        currentGatt = null
        gattCallback = null
        
        _connectionState.value = BleConnectionState.DISCONNECTED
        _connectedDevice.value = null
        _batteryLevel.value = null
    }
    
    /**
     * 스캔 필터 생성
     */
    private fun createScanFilters(): List<ScanFilter> {
        // UUID 필터 임시 제거 - 모든 BLE 기기 스캔
        return emptyList()
        
        // 원래 코드 (WISH RING만 필터링)
        /*
        return listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID_OBJ))
                .build()
        )
        */
    }
    
    /**
     * 스캔 설정 생성
     */
    private fun createScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()
    }
    
    /**
     * 스캔 콜백 생성
     */
    private fun createScanCallback(): ScanCallback {
        return object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                scope.launch {
                    handleScanResult(result)
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error code: $errorCode")
                isScanning = false
            }
        }
    }
    
    /**
     * 스캔 결과 처리
     */
    private suspend fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val serviceUuids = result.scanRecord?.serviceUuids?.map { it.toString() }
        
        // 디버그 로깅 추가
        Log.d(TAG, "🎯 기기 발견: ${device.name ?: "Unknown"} (${device.address})")
        Log.d(TAG, "   📡 RSSI: $rssi")
        Log.d(TAG, "   📋 Service UUIDs: $serviceUuids")
        
        val discoveredDevice = DiscoveredDevice(
            device = device,
            rssi = rssi,
            serviceUuids = serviceUuids
        )
        
        // 발견된 기기 목록 업데이트
        val currentDevices = _discoveredDevices.value.toMutableList()
        val existingIndex = currentDevices.indexOfFirst { it.device.address == device.address }
        
        if (existingIndex >= 0) {
            currentDevices[existingIndex] = discoveredDevice
        } else {
            currentDevices.add(discoveredDevice)
        }
        
        _discoveredDevices.value = currentDevices
    }
    
    /**
     * GATT 콜백 생성
     */
    private fun createGattCallback(device: BluetoothDevice): BluetoothGattCallback {
        return object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothGatt.STATE_CONNECTED -> {
                        Log.d(TAG, "GATT connected")
                        _connectedDevice.value = device
                        gatt?.discoverServices()
                    }
                    BluetoothGatt.STATE_DISCONNECTED -> {
                        Log.d(TAG, "GATT disconnected")
                        if (_connectionState.value == BleConnectionState.CONNECTED) {
                            // 예상치 못한 연결 끊김
                            Log.w(TAG, "Unexpected disconnection, starting auto reconnect")
                            _connectionState.value = BleConnectionState.DISCONNECTED
                            startAutoReconnect()
                        }
                        cleanup()
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Services discovered")
                    
                    scope.launch {
                        // 최종 검증
                        val validation = deviceValidator.validateWishRingDevice(device, null)
                        
                        if (validation.isValid) {
                            _connectionState.value = BleConnectionState.CONNECTED
                            Log.d(TAG, "Device validation successful")
                        } else {
                            Log.w(TAG, "Device validation failed: ${validation.errorMessage}")
                            _connectionState.value = BleConnectionState.ERROR
                        }
                    }
                } else {
                    Log.e(TAG, "Service discovery failed: $status")
                    _connectionState.value = BleConnectionState.ERROR
                }
            }
            
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
            ) {
                // 특성 변화 처리 (버튼 클릭, 배터리 변화 등)
                characteristic?.let { char ->
                    when (char.uuid) {
                        BleConstants.BATTERY_CHAR_UUID_OBJ -> {
                            val batteryData = char.value
                            if (batteryData.isNotEmpty()) {
                                val level = batteryData[0].toInt() and 0xFF
                                _batteryLevel.value = level
                                Log.d(TAG, "Battery level updated via notification: $level%")
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 블루투스 활성화 상태 확인
     */
    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 현재 배터리 레벨 가져오기 (placeholder)
     */
    private suspend fun getCurrentBatteryLevel(): Int? {
        // TODO: MrdProtocolAdapter를 통해 실제 배터리 정보 요청
        return null
    }
    
    /**
     * 연결 상태 확인 (placeholder)
     */
    private suspend fun checkConnectionHealth(): Boolean {
        // TODO: 간단한 통신으로 연결 상태 확인
        return _connectionState.value == BleConnectionState.CONNECTED
    }
    
    /**
     * 리소스 정리
     */
    fun destroy() {
        scope.cancel()
        stopScanning()
        stopBatteryMonitoring()
        stopHealthCheck()
        reconnectJob?.cancel()
        cleanup()
    }
}