package com.wishring.app.ble

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
import com.wishring.app.core.util.Constants
import com.wishring.app.domain.repository.*
import com.wishring.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineDispatcher
import com.wishring.app.di.IoDispatcher
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Implementation of BleRepository
 * Manages BLE operations for WISH RING device communication
 */
@Singleton
class BleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mrdProtocolAdapter: MrdProtocolAdapter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BleRepository {
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private var currentGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null
    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    private val _buttonPressEvents = MutableSharedFlow<ButtonPressEvent>()
    private val _notifications = MutableSharedFlow<BleNotification>()
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    
    private val coroutineScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val discoveredDevices = ConcurrentHashMap<String, BleDevice>()
    private var isScanning = false
    
    // Service and Characteristic UUIDs
    private val serviceUuid = UUID.fromString(Constants.BLE_SERVICE_UUID)
    private val counterCharUuid = UUID.fromString(Constants.BLE_COUNTER_CHAR_UUID)
    private val batteryCharUuid = UUID.fromString(Constants.BLE_BATTERY_CHAR_UUID)
    private val resetCharUuid = UUID.fromString(Constants.BLE_RESET_CHAR_UUID)
    
    init {
        // Initialize MRD Protocol Adapter
        mrdProtocolAdapter.initialize { command ->
            sendBleCommand(command)
        }
    }
    
    @SuppressLint("MissingPermission")
    override fun startScanning(timeout: Long): Flow<BleDevice> = callbackFlow {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            close(IllegalStateException("Bluetooth not available or disabled"))
            return@callbackFlow
        }
        
        if (bluetoothLeScanner == null) {
            close(IllegalStateException("BLE scanner not available"))
            return@callbackFlow
        }
        
        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val rssi = result.rssi
                val name = device.name ?: "Unknown Device"
                
                // Filter for WISH RING devices
                if (name.startsWith(Constants.BLE_DEVICE_NAME_PREFIX)) {
                    val bleDevice = BleDevice(
                        name = name,
                        address = device.address,
                        rssi = rssi,
                        isConnectable = result.isConnectable,
                        isBonded = device.bondState == BluetoothDevice.BOND_BONDED
                    )
                    
                    // Avoid duplicates
                    if (!discoveredDevices.containsKey(device.address)) {
                        discoveredDevices[device.address] = bleDevice
                        trySend(bleDevice)
                    }
                }
            }
            
            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("Scan failed with error: $errorCode"))
            }
        }
        
        val scanFilters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(serviceUuid))
                .build()
        )
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        try {
            isScanning = true
            bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
            
            // Stop scanning after timeout
            if (timeout > 0) {
                kotlinx.coroutines.delay(timeout)
                if (isScanning) {
                    bluetoothLeScanner.stopScan(scanCallback)
                    isScanning = false
                }
            }
            
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            if (isScanning) {
                try {
                    bluetoothLeScanner.stopScan(scanCallback)
                    isScanning = false
                } catch (e: Exception) {
                    // Ignore errors when stopping scan
                }
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun stopScanning() {
        if (isScanning && bluetoothLeScanner != null) {
            isScanning = false
            // Stop scan callback is handled in the flow
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun connectDevice(deviceAddress: String): Boolean = 
        suspendCancellableCoroutine { continuation ->
            if (bluetoothAdapter == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            try {
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                currentDevice = device
                _connectionState.value = BleConnectionState.CONNECTING
                
                val gattCallback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        when (newState) {
                            BluetoothGatt.STATE_CONNECTED -> {
                                _connectionState.value = BleConnectionState.CONNECTED
                                gatt?.discoverServices()
                            }
                            BluetoothGatt.STATE_DISCONNECTED -> {
                                _connectionState.value = BleConnectionState.DISCONNECTED
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
                            coroutineScope.launch {
                                enableNotifications()
                            }
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
                        characteristic?.let { char ->
                            handleCharacteristicChanged(char)
                        }
                    }
                    
                    override fun onCharacteristicRead(
                        gatt: BluetoothGatt?,
                        characteristic: BluetoothGattCharacteristic?,
                        status: Int
                    ) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            characteristic?.let { char ->
                                handleCharacteristicRead(char)
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
                _connectionState.value = BleConnectionState.ERROR
                continuation.resume(false)
            }
            
            continuation.invokeOnCancellation {
                currentGatt?.close()
                currentGatt = null
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    
    @SuppressLint("MissingPermission")
    override suspend fun disconnectDevice() {
        _connectionState.value = BleConnectionState.DISCONNECTING
        currentGatt?.disconnect()
        currentGatt?.close()
        currentGatt = null
        currentDevice = null
        _connectionState.value = BleConnectionState.DISCONNECTED
    }
    
    override suspend fun isDeviceConnected(): Boolean {
        return _connectionState.value == BleConnectionState.CONNECTED
    }
    
    override fun getConnectionState(): Flow<BleConnectionState> = _connectionState.asStateFlow()
    
    override suspend fun getConnectedDevice(): BleDevice? {
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
    override suspend fun sendWishCount(count: Int): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            val data = ByteArray(4)
            data[0] = (count shr 24).toByte()
            data[1] = (count shr 16).toByte()
            data[2] = (count shr 8).toByte()
            data[3] = count.toByte()
            
            characteristic.value = data
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendWishText(text: String): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            // Truncate text to fit BLE packet size
            val truncatedText = text.take(20)
            characteristic.value = truncatedText.toByteArray()
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendTargetCount(target: Int): Boolean {
        return sendWishCount(target) // Use same characteristic
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun sendCompletionStatus(isCompleted: Boolean): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            characteristic.value = byteArrayOf(if (isCompleted) 1 else 0)
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    override suspend fun syncAllData(
        wishCount: Int,
        wishText: String,
        targetCount: Int,
        isCompleted: Boolean
    ): Boolean {
        return try {
            sendWishCount(wishCount) &&
            sendWishText(wishText) &&
            sendTargetCount(targetCount) &&
            sendCompletionStatus(isCompleted)
        } catch (e: Exception) {
            false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun readWishCount(): Int? {
        val gatt = currentGatt ?: return null
        val service = gatt.getService(serviceUuid) ?: return null
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return null
        
        return try {
            if (gatt.readCharacteristic(characteristic)) {
                // Result will be handled in callback
                null // For now, return null
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun readButtonPressCount(): Int? {
        // Implementation would read from button press characteristic
        return null
    }
    
    override fun subscribeToButtonPress(): Flow<ButtonPressEvent> = _buttonPressEvents
    
    override fun subscribeToNotifications(): Flow<BleNotification> = _notifications
    
    @SuppressLint("MissingPermission")
    override suspend fun getBatteryLevel(): Int? {
        val gatt = currentGatt ?: return null
        val service = gatt.getService(serviceUuid) ?: return null
        val characteristic = service.getCharacteristic(batteryCharUuid) ?: return null
        
        return try {
            if (gatt.readCharacteristic(characteristic)) {
                _batteryLevel.value
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override fun subscribeToBatteryLevel(): Flow<Int> = 
        _batteryLevel.filterNotNull()
    
    override suspend fun getFirmwareVersion(): String? {
        // Implementation would read firmware version characteristic
        return "1.0.0" // Mock version
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun updateDeviceTime(): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            val currentTime = System.currentTimeMillis()
            val timeBytes = ByteArray(8)
            for (i in 0..7) {
                timeBytes[i] = (currentTime shr (i * 8)).toByte()
            }
            
            characteristic.value = timeBytes
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun resetDevice(): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(resetCharUuid) ?: return false
        
        try {
            characteristic.value = byteArrayOf(0xFF.toByte())
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun enableNotifications(): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            val success = gatt.setCharacteristicNotification(characteristic, true)
            if (success) {
                val descriptor = characteristic.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                )
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }
            }
            return success
        } catch (e: Exception) {
            return false
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun disableNotifications(): Boolean {
        val gatt = currentGatt ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            return gatt.setCharacteristicNotification(characteristic, false)
        } catch (e: Exception) {
            return false
        }
    }
    
    override suspend fun setLedColor(color: String): Boolean {
        // Implementation would send LED color command
        return true
    }
    
    override suspend fun setVibrationPattern(pattern: VibrationPattern): Boolean {
        // Implementation would send vibration pattern command
        return true
    }
    
    override suspend fun testConnection(): Boolean {
        return isDeviceConnected() && currentGatt?.readRemoteRssi() == true
    }
    
    override suspend fun getDeviceInfo(): DeviceInfo? {
        val device = currentDevice ?: return null
        
        return DeviceInfo(
            name = device.name ?: "Unknown",
            address = device.address,
            firmwareVersion = getFirmwareVersion() ?: "Unknown",
            hardwareVersion = "1.0",
            batteryLevel = getBatteryLevel() ?: 0,
            serialNumber = null,
            manufacturer = "WISH RING"
        )
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun clearBondedDevices() {
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            if (device.name?.startsWith(Constants.BLE_DEVICE_NAME_PREFIX) == true) {
                try {
                    device.javaClass.getMethod("removeBond").invoke(device)
                } catch (e: Exception) {
                    // Ignore errors
                }
            }
        }
    }

    
    override suspend fun findDevice(enable: Boolean): Boolean {
        return withContext(ioDispatcher) {
            try {
                if (!_connectionState.value.isConnected()) {
                    return@withContext false
                }
                
                // Use SDK's find device feature (LED flash or vibration)
                if (enable) {
                    // Start device finding - activate LED/vibration
                    mrdProtocolAdapter.activateDeviceFinder()
                } else {
                    // Stop device finding
                    mrdProtocolAdapter.deactivateDeviceFinder()
                }
                true
            } catch (e: Exception) {
                Log.e("BleRepositoryImpl", "Error in findDevice: ${e.message}", e)
                false
            }
        }
    }
    
    private fun handleCharacteristicChanged(characteristic: BluetoothGattCharacteristic) {
        val data = characteristic.value
        if (data.isNotEmpty()) {
            // Forward data to MRD Protocol Adapter for parsing
            mrdProtocolAdapter.onDataReceived(data)
        }
        
        // Keep existing logic for backward compatibility
        when (characteristic.uuid) {
            counterCharUuid -> {
                // Handle button press or count change
                if (data.isNotEmpty()) {
                    val pressCount = data[0].toInt()
                    val event = ButtonPressEvent(
                        timestamp = System.currentTimeMillis(),
                        pressCount = pressCount,
                        pressType = when (pressCount) {
                            1 -> PressType.SINGLE
                            2 -> PressType.DOUBLE
                            3 -> PressType.TRIPLE
                            else -> PressType.LONG
                        }
                    )
                    _buttonPressEvents.tryEmit(event)
                }
            }
            batteryCharUuid -> {
                // Handle battery level update
                if (data.isNotEmpty()) {
                    _batteryLevel.value = data[0].toInt().coerceIn(0, 100)
                }
            }
        }
    }
    
    private fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        when (characteristic.uuid) {
            batteryCharUuid -> {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    _batteryLevel.value = data[0].toInt().coerceIn(0, 100)
                }
            }
        }
    }
    
    /**
     * Send BLE command to device
     * Helper method used by MrdProtocolAdapter
     */
    @SuppressLint("MissingPermission")
    private fun sendBleCommand(command: ByteArray) {
        val gatt = currentGatt ?: return
        val service = gatt.getService(serviceUuid) ?: return
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return
        
        try {
            characteristic.value = command
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    // ====== MRD SDK 새로운 기능들 구현 ======
    
    override suspend fun getSystemInfo(type: SystemInfoType): String? {
        return try {
            mrdProtocolAdapter.getSystemInfo(type)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun setScreenBrightness(brightness: Int): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override suspend fun factoryReset(): Boolean {
        // TODO: Implement with actual SDK
        return resetDevice()
    }
    
    override suspend fun setUserProfile(userProfile: UserProfile): Boolean {
        return try {
            mrdProtocolAdapter.setUserProfile(userProfile)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getUserProfile(): UserProfile? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override suspend fun setSportTarget(steps: Int): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override suspend fun getSportTarget(): Int? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override suspend fun getLatestHeartRate(): HeartRateData? {
        return try {
            mrdProtocolAdapter.getLatestHeartRate()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getHeartRateHistory(days: Int): List<HeartRateData> {
        // TODO: Implement with actual SDK
        return emptyList()
    }
    
    override suspend fun getLatestBloodPressure(): BloodPressureData? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override suspend fun getSleepData(date: String?): SleepData? {
        return try {
            mrdProtocolAdapter.getSleepData(date)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getStepData(date: String?): StepData? {
        return try {
            mrdProtocolAdapter.getStepData(date)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getTemperatureData(): TemperatureData? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override suspend fun getEcgData(): EcgData? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override suspend fun getBloodOxygenData(): BloodOxygenData? {
        // TODO: Implement with actual SDK
        return null
    }
    
    override fun startRealTimeHeartRate(): Flow<HeartRateData> {
        return mrdProtocolAdapter.startRealTimeHeartRate()
    }
    
    override suspend fun stopRealTimeHeartRate(): Boolean {
        // Stop is handled automatically when Flow is cancelled
        return true
    }
    
    override fun startRealTimeEcg(): Flow<EcgData> {
        return mrdProtocolAdapter.startRealTimeEcg()
    }
    
    override suspend fun stopRealTimeEcg(): Boolean {
        // Stop is handled automatically when Flow is cancelled
        return true
    }
    
    override fun startRealTimeBloodPressure(): Flow<BloodPressureData> {
        // TODO: Implement with actual SDK
        return emptyFlow()
    }
    
    override suspend fun stopRealTimeBloodPressure(): Boolean {
        return true
    }
    
    override suspend fun sendAppNotification(notification: AppNotification): Boolean {
        return try {
            mrdProtocolAdapter.sendAppNotification(notification)
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun setDeviceLanguage(language: String): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override suspend fun setUnitPreferences(units: UnitPreferences): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override suspend fun setRaiseWristWake(enabled: Boolean): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override suspend fun setAutoHeartRateMonitoring(enabled: Boolean, interval: Int): Boolean {
        // TODO: Implement with actual SDK
        return true
    }
    
    override fun subscribeToHealthDataUpdates(): Flow<HealthDataUpdate> {
        return mrdProtocolAdapter.subscribeToHealthDataUpdates()
    }
    
    override fun subscribeToDeviceStatus(): Flow<DeviceStatus> {
        return mrdProtocolAdapter.subscribeToDeviceStatus()
    }
}