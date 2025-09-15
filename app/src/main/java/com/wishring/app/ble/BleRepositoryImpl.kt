package com.wishring.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import com.wishring.app.ble.managers.BleConnectionManager
import com.wishring.app.ble.managers.BleDataManager
import com.wishring.app.ble.managers.BleDeviceManager
import com.wishring.app.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineDispatcher
import com.wishring.app.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of BleRepository
 * Manages BLE operations for WISH RING device communication
 * Simplified to handle only count and battery operations
 */
@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mrdProtocolAdapter: MrdProtocolAdapter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BleRepository {
    
    // Connection state
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    // Manager instances
    private val connectionManager = BleConnectionManager(
        context = context,
        connectionStateFlow = _connectionState,
        onCharacteristicChanged = ::handleCharacteristicChanged,
        onCharacteristicRead = ::handleCharacteristicRead
    )
    
    private val dataManager = BleDataManager(
        getGatt = { connectionManager.currentGatt }
    )
    
    private val deviceManager = BleDeviceManager(
        ioDispatcher = ioDispatcher,
        getConnectionState = { _connectionState.value },
        getCurrentDevice = { connectionManager.currentDevice },
        getBatteryLevel = { dataManager.getBatteryLevel() }
    )
    
    companion object {
        private const val TAG = "WishRing_BleRepo"
    }
    
    init {
        // Initialize MRD Protocol Adapter
        mrdProtocolAdapter.initialize { command ->
            dataManager.sendBleCommand(command)
        }
        
        // Connection state is already managed via _connectionState passed to connectionManager
        android.util.Log.d(TAG, "✅ BLE Repository 초기화 완료")
    }
    
    // Connection Operations
    override fun startScanning(timeout: Long): Flow<BleDevice> {
        Log.d(TAG, "🔍 BleRepository: 스캔 시작 요청 (timeout: ${timeout}ms)")
        return connectionManager.startScanning(timeout)
            .onStart { 
                Log.d(TAG, "✅ BleRepository: 스캔 Flow 시작됨") 
            }
            .onEach { device ->
                Log.d(TAG, "📱 BleRepository: 기기 발견 - ${device.name} (${device.address})")
                
                // 모든 기기 동등하게 처리 (사용자가 선택)
            }
            .buffer(capacity = 10) // 버퍼 추가로 기기 손실 방지
            .onCompletion { 
                Log.d(TAG, "🏁 BleRepository: 스캔 Flow 완료") 
            }
            .catch { e ->
                Log.e(TAG, "❌ BleRepository: 스캔 오류 - ${e.message}", e)
                throw e
            }
    }
    
    override suspend fun stopScanning() {
        connectionManager.stopScanning()
    }
    
    override suspend fun connectDevice(deviceAddress: String): Boolean {
        return connectionManager.connectDevice(deviceAddress) {
            // Notifications will be enabled separately if needed
        }
    }
    
    override suspend fun disconnectDevice() {
        connectionManager.disconnectDevice()
    }
    
    override suspend fun isDeviceConnected(): Boolean {
        return connectionManager.isDeviceConnected()
    }
    
    override suspend fun getConnectedDevice(): BleDevice? {
        return connectionManager.getConnectedDevice()
    }
    
    // Data Operations - Wish Count
    override suspend fun sendWishCount(count: Int): Boolean {
        return dataManager.sendWishCount(count)
    }
    
    override suspend fun sendWishText(text: String): Boolean {
        return dataManager.sendWishText(text)
    }
    
    override suspend fun sendTargetCount(target: Int): Boolean {
        return dataManager.sendTargetCount(target)
    }
    
    override suspend fun sendCompletionStatus(isCompleted: Boolean): Boolean {
        return dataManager.sendCompletionStatus(isCompleted)
    }
    
    override suspend fun syncAllData(
        wishCount: Int,
        wishText: String,
        targetCount: Int,
        isCompleted: Boolean
    ): Boolean {
        return dataManager.syncAllData(wishCount, wishText, targetCount, isCompleted)
    }
    
    override suspend fun readWishCount(): Int? {
        return dataManager.readWishCount()
    }
    
    override suspend fun readButtonPressCount(): Int? {
        return dataManager.readButtonPressCount()
    }
    
    // Battery Operations
    override suspend fun getBatteryLevel(): Int? {
        return dataManager.getBatteryLevel()
    }
    
    override fun getBatteryLevelFlow(): StateFlow<Int?> {
        return dataManager.batteryLevel
    }
    
    override suspend fun requestBatteryUpdate(): Result<Unit> {
        return deviceManager.requestBatteryUpdate()
    }
    
    // Event Streams
    override val buttonPressEvents: Flow<ButtonPressEvent> = dataManager.buttonPressEvents
    
    override val notifications: Flow<BleNotification> = dataManager.notifications
    
    // Counter increments from MRD SDK (HEART events)
    override val counterIncrements: Flow<Int> = merge(
        deviceManager.counterIncrements, // Mock increments for testing
        mrdProtocolAdapter.subscribeToButtonPresses() // Real HEART events from device
    )
    
    // Device Operations
    override suspend fun updateDeviceTime(): Boolean {
        return dataManager.updateDeviceTime()
    }
    
    override suspend fun resetDevice(): Boolean {
        return dataManager.resetDevice()
    }
    
    override suspend fun enableNotifications(): Boolean {
        return connectionManager.enableNotifications()
    }
    
    override suspend fun disableNotifications(): Boolean {
        return connectionManager.disableNotifications()
    }
    
    override suspend fun clearBondedDevices() {
        connectionManager.clearBondedDevices()
    }
    
    override suspend fun testConnection(): Boolean {
        return connectionManager.testConnection()
    }
    
    // Internal handlers
    private fun handleCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        val data = characteristic.value
        Log.d("BleRepositoryImpl", "========================================")
        Log.d("BleRepositoryImpl", "🚨 [BLE 특성 변경] UUID: ${characteristic.uuid}")
        Log.d("BleRepositoryImpl", "📦 [BLE 데이터 크기]: ${data?.size ?: 0} bytes")
        Log.d("BleRepositoryImpl", "📊 [BLE 원시 데이터]: ${data?.joinToString(" ") { "%02x".format(it) } ?: "null"}")
        Log.d("BleRepositoryImpl", "========================================")
        
        if (data != null && data.isNotEmpty()) {
            // Forward to MRD adapter for parsing
            Log.d("BleRepositoryImpl", "➡️ MRD 어댑터로 데이터 전달 중...")
            mrdProtocolAdapter.onDataReceived(data)
        } else {
            Log.w("BleRepositoryImpl", "⚠️ 빈 데이터 또는 null 데이터 수신")
        }
        
        // Also handle in data manager for legacy support
        dataManager.handleCharacteristicChanged(characteristic, mrdProtocolAdapter)
    }
    
    private fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        dataManager.handleCharacteristicRead(characteristic)
    }
}