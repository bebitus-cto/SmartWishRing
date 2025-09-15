package com.wishring.app.ble.managers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.wishring.app.ble.BleConstants
import com.wishring.app.core.util.Constants
import com.wishring.app.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Manages BLE connection and scanning operations
 */
class BleConnectionManager(
    private val context: Context,
    private val connectionStateFlow: MutableStateFlow<BleConnectionState>,
    private val onCharacteristicChanged: (BluetoothGattCharacteristic) -> Unit,
    private val onCharacteristicRead: (BluetoothGattCharacteristic) -> Unit
) {
    companion object {
        private const val TAG = "BleConnectionManager"
    }
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    var currentGatt: BluetoothGatt? = null
        private set
    var currentDevice: BluetoothDevice? = null
        private set
    
    private val discoveredDevices = ConcurrentHashMap<String, BleDevice>()
    private val isScanning = AtomicBoolean(false)
    
    private val serviceUuid = UUID.fromString(Constants.BLE_SERVICE_UUID)
    private val counterCharUuid = UUID.fromString(Constants.BLE_COUNTER_CHAR_UUID)
    
    init {
        Log.d(TAG, "🔥 BleConnectionManager 초기화 - 올바른 UUID 사용")
        Log.d(TAG, "📡 Service UUID: $serviceUuid")
        Log.d(TAG, "📡 Counter UUID: $counterCharUuid")
    }
    
    @SuppressLint("MissingPermission")
    fun startScanning(timeout: Long): Flow<BleDevice> = callbackFlow {
        Log.d(TAG, "🔍 === BLE 스캔 시작 (MRD SDK 방식) ===")
        Log.d(TAG, "📍 타임아웃: ${timeout}ms")
        Log.d(TAG, "🔄 Legacy startLeScan API 사용 (H13 기기 지원)")
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "❌ Bluetooth not available or disabled")
            close()
            return@callbackFlow
        }
        
        discoveredDevices.clear()
        
        // MRD SDK 방식: BluetoothAdapter.LeScanCallback 사용
        val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            var name = device.name
            
            // WISH RING 기기는 이름이 없을 수도 있으므로 스캔 레코드 확인
            // 실제 기기는 서비스 UUID나 제조사 데이터로 식별해야 함
            
            // 이름이 없는 기기는 무시 (H13 제외)
            if (name.isNullOrEmpty()) {
                // 로그 스팸 방지 - 주석 처리
                // Log.d(TAG, "🔸 이름 없는 기기 무시: ${device.address}")
                return@LeScanCallback
            }
            
            // 디버그 로깅
            Log.d(TAG, "🎯 기기 발견 (Legacy): $name (${device.address})")
            Log.d(TAG, "   📡 RSSI: $rssi")
            
            // Scan Record 파싱 (디버그용)
            if (scanRecord != null && scanRecord.isNotEmpty()) {
                val scanRecordHex = scanRecord.take(30).joinToString(" ") { "%02X".format(it) }
                Log.d(TAG, "   📦 Scan Record: $scanRecordHex...")
            }
            
            // 모든 BLE 기기 로깅 (사용자가 선택)
            Log.d(TAG, "🎯 기기 발견: $name")
            Log.d(TAG, "   📍 주소: ${device.address}")
            Log.d(TAG, "   📡 RSSI: $rssi dBm")
            Log.d(TAG, "   🔗 본딩: ${when(device.bondState) {
                BluetoothDevice.BOND_BONDED -> "본딩됨"
                BluetoothDevice.BOND_BONDING -> "본딩중"
                else -> "본딩안됨"
            }}")
            
            // BleDevice 생성
            val bleDevice = BleDevice(
                name = name,
                address = device.address,
                rssi = rssi,
                isConnectable = true, // Legacy scan에서는 항상 true로 가정
                isBonded = device.bondState == BluetoothDevice.BOND_BONDED
            )
            
            // 중복 방지
            if (!discoveredDevices.containsKey(device.address)) {
                discoveredDevices[device.address] = bleDevice
                
                // Flow로 전송 시도
                val sendResult = trySend(bleDevice)
                
                if (sendResult.isSuccess) {
                    Log.d(TAG, "📤 UI로 전송 성공: $name (${device.address})")
                } else if (sendResult.isFailure) {
                    Log.e(TAG, "❌ UI로 전송 실패: $name - ${sendResult.exceptionOrNull()?.message}")
                } else if (sendResult.isClosed) {
                    Log.e(TAG, "❌ Flow가 닫혀있음: $name")
                }
            } else {
                Log.d(TAG, "🔄 이미 발견된 기기 스킵: $name")
            }
        }
        
        try {
            Log.d(TAG, "📱 Legacy BLE 스캔 시작 중...")
            
            // 기존 스캔 중지 (안전을 위해)
            try {
                bluetoothAdapter.stopLeScan(leScanCallback)
                Thread.sleep(100) // 짧은 대기
            } catch (e: Exception) {
                // 무시
            }
            
            // 새 스캔 시작
            val scanStarted = bluetoothAdapter.startLeScan(leScanCallback)
            
            if (!scanStarted) {
                Log.e(TAG, "❌ startLeScan 실패!")
                close(IllegalStateException("Failed to start legacy BLE scan"))
                return@callbackFlow
            }
            
            isScanning.set(true)
            Log.d(TAG, "✅ Legacy 스캔 시작 성공!")
            Log.d(TAG, "💡 H13 기기를 찾고 있습니다...")
            
            // 타임아웃 후 스캔 중지
            if (timeout > 0) {
                kotlinx.coroutines.delay(timeout)
                if (isScanning.get()) {
                    Log.d(TAG, "⏱️ 스캔 타임아웃 (${timeout}ms) - 중지")
                    Log.d(TAG, "📊 발견된 기기 수: ${discoveredDevices.size}")
                    
                    // 발견된 기기 목록 출력
                    discoveredDevices.forEach { (address, device) ->
                        Log.d(TAG, "  📱 ${device.name} (${address}) - RSSI: ${device.rssi}")
                    }
                    
                    bluetoothAdapter.stopLeScan(leScanCallback)
                    isScanning.set(false)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 스캔 오류: ${e.message}", e)
            close(e)
        }
        
        awaitClose {
            if (isScanning.get()) {
                try {
                    Log.d(TAG, "🛑 스캔 중지 (awaitClose)")
                    bluetoothAdapter.stopLeScan(leScanCallback)
                    isScanning.set(false)
                } catch (e: Exception) {
                    Log.e(TAG, "스캔 중지 중 오류: ${e.message}")
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun stopScanning() {
        Log.d(TAG, "🛑 stopScanning 호출됨")
        if (isScanning.get()) {
            isScanning.set(false)
            // Legacy scan stop은 Flow의 awaitClose에서 처리됨
            Log.d(TAG, "✅ 스캔 중지 플래그 설정")
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun connectDevice(deviceAddress: String, onServicesDiscovered: () -> Unit): Boolean = 
        suspendCancellableCoroutine { continuation ->
            if (bluetoothAdapter == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            try {
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                currentDevice = device
                connectionStateFlow.value = BleConnectionState.CONNECTING
                
                val gattCallback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        when (newState) {
                            BluetoothGatt.STATE_CONNECTED -> {
                                connectionStateFlow.value = BleConnectionState.CONNECTED
                                gatt?.discoverServices()
                            }
                            BluetoothGatt.STATE_DISCONNECTED -> {
                                connectionStateFlow.value = BleConnectionState.DISCONNECTED
                                currentGatt?.close()
                                currentGatt = null
                                if (continuation.isActive) {
                                    continuation.resume(false)
                                }
                            }
                        }
                    }
                    
                    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            currentGatt = gatt
                            onServicesDiscovered()
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        } else {
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }
                    
                    override fun onCharacteristicChanged(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?
                    ) {
                        Log.d(TAG, "🔥 [BLE 콜백] onCharacteristicChanged 호출됨!")
                        characteristic?.let { char ->
                            Log.d(TAG, "🔥 [BLE 콜백] 특성 변경 - UUID: ${char.uuid}")
                            Log.d(TAG, "🔥 [BLE 콜백] 데이터 크기: ${char.value?.size ?: 0} bytes")
                            onCharacteristicChanged(char)
                        } ?: Log.w(TAG, "⚠️ [BLE 콜백] characteristic이 null")
                    }
                    
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            characteristic?.let { char ->
                                onCharacteristicRead(char)
                            }
                        }
                    }
                    
                    override fun onCharacteristicWrite(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        // Handle write success/failure
                    }
                }
                
                currentGatt = device.connectGatt(context, false, gattCallback)
                
            } catch (e: Exception) {
                connectionStateFlow.value = BleConnectionState.ERROR
                continuation.resume(false)
            }
            
            continuation.invokeOnCancellation {
                currentGatt?.close()
                currentGatt = null
                connectionStateFlow.value = BleConnectionState.DISCONNECTED
            }
        }
    
    @SuppressLint("MissingPermission")
    suspend fun disconnectDevice() {
        connectionStateFlow.value = BleConnectionState.DISCONNECTING
        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null
        currentDevice = null
        connectionStateFlow.value = BleConnectionState.DISCONNECTED
    }
    
    suspend fun isDeviceConnected(): Boolean {
        return connectionStateFlow.value == BleConnectionState.CONNECTED
    }
    
    suspend fun getConnectedDevice(): BleDevice? {
        return currentDevice?.let { device ->
            BleDevice(
                name = device.name ?: "Unknown",
                address = device.address,
                rssi = 0, // Not available after connection
                isConnectable = true,
                isBonded = device.bondState == BluetoothDevice.BOND_BONDED
            )
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun enableNotifications(): Boolean {
        val gatt = currentGatt ?: run {
            Log.w(TAG, "⚠️ GATT 연결이 null - 알림 활성화 불가")
            return false
        }
        
        Log.d(TAG, "🔔 BLE 알림 및 연결 상태 전체 점검 시작...")
        
        // 1. GATT 연결 상태 확인
        Log.d(TAG, "📡 현재 GATT 연결 상태: ${connectionStateFlow.value}")
        Log.d(TAG, "📡 GATT 객체 연결됨: ${gatt != null}")
        Log.d(TAG, "📡 현재 연결된 기기: ${currentDevice?.address ?: "없음"}")
        
        // 2. 서비스 발견 상태 확인
        val services = gatt.services
        if (services.isNullOrEmpty()) {
            Log.e(TAG, "❌ 서비스가 발견되지 않음 - 서비스 재탐색 시도")
            val discoveryResult = gatt.discoverServices()
            Log.d(TAG, "🔍 서비스 재탐색 결과: $discoveryResult")
            delay(2000) // 서비스 발견 대기
        }
        
        Log.d(TAG, "📡 연결된 GATT 서비스 수: ${gatt.services?.size ?: 0}")
        
        // 3. 모든 서비스와 특성 상세 분석
        var notificationEnabledCount = 0
        var totalNotifiableCharacteristics = 0
        
        gatt.services?.forEachIndexed { serviceIndex, service ->
            Log.d(TAG, "🔍 [$serviceIndex] 서비스: ${service.uuid}")
            Log.d(TAG, "📋 [$serviceIndex] 특성 수: ${service.characteristics?.size ?: 0}")
            
            // MRD SDK 주요 서비스인지 확인
            val isMrdService = service.uuid.toString().equals("f000efe0-0451-4000-0000-00000000b000", ignoreCase = true)
            if (isMrdService) {
                Log.i(TAG, "🎯 MRD SDK 핵심 서비스 발견: ${service.uuid}")
            }
            
            service.characteristics?.forEachIndexed { charIndex, characteristic ->
                Log.d(TAG, "🔎 [$serviceIndex-$charIndex] 특성: ${characteristic.uuid}")
                Log.d(TAG, "🏷️ [$serviceIndex-$charIndex] 속성: 0x${characteristic.properties.toString(16)}")
                
                // 특성 속성 상세 분석
                val hasNotify = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                val hasIndicate = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                val hasRead = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0
                val hasWrite = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                val hasWriteNoResponse = (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                
                Log.d(TAG, "📊 [$serviceIndex-$charIndex] READ:$hasRead write:$hasWrite writeNoResp:$hasWriteNoResponse notify:$hasNotify indicate:$hasIndicate")
                
                // MRD SDK 핵심 특성들 확인
                val isMrdCounterChar = characteristic.uuid.toString().equals("f000efe3-0451-4000-0000-00000000b000", ignoreCase = true)
                val isMrdWriteChar = characteristic.uuid.toString().equals("f000efe1-0451-4000-0000-00000000b000", ignoreCase = true)
                
                if (isMrdCounterChar) {
                    Log.i(TAG, "🎯 MRD SDK 카운터 특성 발견: ${characteristic.uuid}")
                    Log.i(TAG, "🎯 카운터 특성 알림 지원: $hasNotify, 인디케이트 지원: $hasIndicate")
                }
                if (isMrdWriteChar) {
                    Log.i(TAG, "🎯 MRD SDK 쓰기 특성 발견: ${characteristic.uuid}")
                }
                
                // 알림 가능한 특성 처리
                if (hasNotify || hasIndicate) {
                    totalNotifiableCharacteristics++
                    Log.d(TAG, "🔔 [$serviceIndex-$charIndex] 알림 가능한 특성 발견")
                    
                    try {
                        // 특성 알림 활성화
                        val notificationSet = gatt.setCharacteristicNotification(characteristic, true)
                        Log.d(TAG, "📱 [$serviceIndex-$charIndex] setCharacteristicNotification 결과: $notificationSet")
                        
                        if (notificationSet) {
                            // CCCD 디스크립터 설정
                            val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                            val descriptor = characteristic.getDescriptor(cccdUuid)
                            
                            if (descriptor != null) {
                                val value = if (hasNotify) {
                                    Log.d(TAG, "📨 [$serviceIndex-$charIndex] NOTIFICATION 모드로 설정")
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                } else {
                                    Log.d(TAG, "📨 [$serviceIndex-$charIndex] INDICATION 모드로 설정")
                                    BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                                }
                                
                                val currentDescriptorValue = descriptor.value
                                Log.d(TAG, "🔄 [$serviceIndex-$charIndex] 현재 CCCD 값: ${currentDescriptorValue?.joinToString(" ") { "%02x".format(it) } ?: "null"}")
                                
                                descriptor.value = value
                                val writeResult = gatt.writeDescriptor(descriptor)
                                Log.d(TAG, "✍️ [$serviceIndex-$charIndex] CCCD 쓰기 요청: $writeResult")
                                Log.d(TAG, "📝 [$serviceIndex-$charIndex] 설정한 CCCD 값: ${value.joinToString(" ") { "%02x".format(it) }}")
                                
                                if (writeResult) {
                                    notificationEnabledCount++
                                    Log.i(TAG, "✅ [$serviceIndex-$charIndex] ${characteristic.uuid} 알림 활성화 성공!")
                                    
                                    // 특별히 MRD 카운터 특성의 경우 추가 확인
                                    if (isMrdCounterChar) {
                                        Log.i(TAG, "🎉 MRD 카운터 특성 알림 활성화 완료! 이제 버튼 누름 이벤트를 받을 수 있습니다.")
                                    }
                                } else {
                                    Log.e(TAG, "❌ [$serviceIndex-$charIndex] CCCD 쓰기 실패")
                                }
                            } else {
                                Log.w(TAG, "⚠️ [$serviceIndex-$charIndex] CCCD 디스크립터 없음")
                            }
                        } else {
                            Log.e(TAG, "❌ [$serviceIndex-$charIndex] setCharacteristicNotification 실패")
                        }
                        
                        // 각 특성 설정 사이에 대기 시간 증가
                        delay(200)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [$serviceIndex-$charIndex] ${characteristic.uuid} 알림 설정 중 예외 발생", e)
                    }
                } else {
                    Log.d(TAG, "📭 [$serviceIndex-$charIndex] 알림 불가 특성 (속성: 0x${characteristic.properties.toString(16)})")
                }
            }
        }
        
        Log.i(TAG, "📊 BLE 알림 설정 완료")
        Log.i(TAG, "📊 전체 알림 가능 특성: $totalNotifiableCharacteristics")
        Log.i(TAG, "📊 알림 활성화 성공: $notificationEnabledCount")
        
        val success = notificationEnabledCount > 0
        if (success) {
            Log.i(TAG, "🎉 BLE 알림 활성화 성공! 스마트링 버튼 이벤트 수신 준비 완료")
            
            // 테스트용 알림 확인
            Log.d(TAG, "🧪 알림 테스트: 이제 스마트링의 무한대(∞) 버튼을 눌러보세요...")
        } else {
            Log.e(TAG, "❌ BLE 알림 활성화 실패 - 모든 특성에서 알림 설정 실패")
        }
        
        return success
    }
    
    @SuppressLint("MissingPermission")
    suspend fun disableNotifications(): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            return gatt.setCharacteristicNotification(characteristic, false)
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun clearBondedDevices() {
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            // Clear all bonded devices to avoid any compatibility issues
            try {
                device.javaClass.getMethod("removeBond").invoke(device)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }
    
    /**
     * Auto-reconnect to last connected device
     * 마지막 연결된 기기로 자동 재연결
     */
    @SuppressLint("MissingPermission")
    suspend fun startAutoReconnect() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting auto-reconnect")
        
        // Get last connected device address if available
        val lastDevice = currentDevice
        
        if (lastDevice != null) {
            Log.d(TAG, "Attempting to reconnect to ${lastDevice.address}")
            
            // Try direct reconnection first
            val reconnected = connectDevice(lastDevice.address) {
                // Callback when services discovered
                Log.d(TAG, "Services rediscovered during reconnection")
            }
            
            if (reconnected) {
                Log.d(TAG, "Successfully reconnected to ${lastDevice.address}")
            } else {
                Log.d(TAG, "Direct reconnection failed, starting scan")
                // If direct reconnection fails, start scanning
                startSmartScanForDevice(lastDevice.address)
            }
        } else {
            Log.d(TAG, "No previous device to reconnect, starting general scan")
            // No previous device, start general scan
            startSmartScan()
        }
    }
    
    /**
     * Smart scan for specific device
     * 특정 기기를 찾기 위한 스마트 스캔
     */
    @SuppressLint("MissingPermission")
    private suspend fun startSmartScanForDevice(targetAddress: String) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting smart scan for device: $targetAddress")
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                if (device.address.equals(targetAddress, ignoreCase = true)) {
                    Log.d(TAG, "Found target device: ${device.address}")
                    
                    // Stop scan and connect
                    bluetoothLeScanner?.stopScan(this)
                    
                    // Attempt connection
                    CoroutineScope(Dispatchers.IO).launch {
                        val connected = connectDevice(device.address) {
                            Log.d(TAG, "Reconnected to target device")
                        }
                        
                        if (!connected) {
                            // If connection fails, resume scanning
                            delay(5000) // Wait before retrying
                            // Don't call recursively to avoid stack overflow
                            // startSmartScanForDevice(targetAddress)
                        }
                    }
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan failed with error: $errorCode")
            }
        }
        
        // Start scanning with filter for the target device
        val scanFilters = listOf(
            ScanFilter.Builder()
                .setDeviceAddress(targetAddress)
                .build()
        )
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        
        // Auto-stop scan after 30 seconds
        delay(30000)
        bluetoothLeScanner?.stopScan(scanCallback)
    }
    
    /**
     * Smart scan with automatic connection to compatible BLE devices
     * 호환 가능한 BLE 기기를 자동으로 찾아 연결하는 스마트 스캔
     */
    @SuppressLint("MissingPermission")
    suspend fun startSmartScan() = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting smart scan for compatible BLE devices")
        
        if (isScanning.get()) {
            Log.d(TAG, "Already scanning")
            return@withContext
        }
        
        isScanning.set(true)
        discoveredDevices.clear()
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val deviceName = device.name
                
                // Accept any BLE device and attempt auto-connection
                if (deviceName != null) {
                    Log.d(TAG, "Found BLE device: $deviceName (${device.address})")
                    
                    // Auto-connect to first compatible device found
                    if (!discoveredDevices.containsKey(device.address)) {
                        discoveredDevices[device.address] = BleDevice(
                            name = deviceName,
                            address = device.address,
                            rssi = result.rssi,
                            isConnectable = result.isConnectable,
                            isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                        )
                        
                        // Stop scan and attempt connection
                        bluetoothLeScanner?.stopScan(this)
                        isScanning.set(false)
                        
                        CoroutineScope(Dispatchers.IO).launch {
                            val connected = connectDevice(device.address) {
                                Log.d(TAG, "Auto-connected to BLE device")
                            }
                            
                            if (!connected) {
                                // If connection fails, resume scanning
                                delay(2000)
                                // Don't call recursively to avoid stack overflow
                                // startSmartScan()
                            }
                        }
                    }
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Smart scan failed: $errorCode")
                isScanning.set(false)
            }
        }
        
        // Scan with filters for better performance
        val scanFilters = mutableListOf<ScanFilter>()
        
        // Add service UUID filter if available
        if (serviceUuid != null) {
            scanFilters.add(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(serviceUuid))
                    .build()
            )
        }
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
        
        // Auto-stop after 30 seconds if no device found
        delay(30000)
        if (isScanning.get()) {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning.set(false)
            Log.d(TAG, "Smart scan timeout")
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun testConnection(): Boolean {
        return isDeviceConnected() && currentGatt?.readRemoteRssi() == true
    }
}