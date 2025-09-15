package com.wishring.app.ble.managers

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import com.wishring.app.core.util.Constants
import com.wishring.app.domain.repository.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Manages BLE data transmission and reception
 */
class BleDataManager(
    private val getGatt: () -> BluetoothGatt?
) {
    private val serviceUuid = UUID.fromString(Constants.BLE_SERVICE_UUID)
    private val counterCharUuid = UUID.fromString(Constants.BLE_COUNTER_CHAR_UUID)
    private val batteryCharUuid = UUID.fromString(Constants.BLE_BATTERY_CHAR_UUID)
    private val resetCharUuid = UUID.fromString(Constants.BLE_RESET_CHAR_UUID)
    
    private val _buttonPressEvents = MutableSharedFlow<ButtonPressEvent>()
    private val _notifications = MutableSharedFlow<BleNotification>()
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    
    val buttonPressEvents: Flow<ButtonPressEvent> = _buttonPressEvents
    val notifications: Flow<BleNotification> = _notifications
    val batteryLevel: StateFlow<Int?> = _batteryLevel
    
    @SuppressLint("MissingPermission")
    suspend fun sendWishCount(count: Int): Boolean {
        val gatt = getGatt() ?: return false
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
    suspend fun sendWishText(text: String): Boolean {
        val gatt = getGatt() ?: return false
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
    suspend fun sendTargetCount(target: Int): Boolean {
        return sendWishCount(target) // Use same characteristic
    }
    
    @SuppressLint("MissingPermission")
    suspend fun sendCompletionStatus(isCompleted: Boolean): Boolean {
        val gatt = getGatt() ?: return false
        val service = gatt.getService(serviceUuid) ?: return false
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return false
        
        try {
            characteristic.value = byteArrayOf(if (isCompleted) 1 else 0)
            return gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            return false
        }
    }
    
    suspend fun syncAllData(
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
    suspend fun readWishCount(): Int? {
        val gatt = getGatt() ?: return null
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
    
    suspend fun readButtonPressCount(): Int? {
        // Implementation would read from button press characteristic
        return null
    }
    
    @SuppressLint("MissingPermission")
    suspend fun getBatteryLevel(): Int? {
        val gatt = getGatt() ?: return null
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
    
    @SuppressLint("MissingPermission")
    suspend fun updateDeviceTime(): Boolean {
        val gatt = getGatt() ?: return false
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
    suspend fun resetDevice(): Boolean {
        val gatt = getGatt() ?: return false
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
    fun sendBleCommand(command: ByteArray) {
        val gatt = getGatt() ?: return
        val service = gatt.getService(serviceUuid) ?: return
        val characteristic = service.getCharacteristic(counterCharUuid) ?: return
        
        try {
            characteristic.value = command
            gatt.writeCharacteristic(characteristic)
        } catch (e: Exception) {
            // Log error
        }
    }
    
    fun handleCharacteristicChanged(characteristic: BluetoothGattCharacteristic, mrdAdapter: Any) {
        val data = characteristic.value
        if (data.isNotEmpty()) {
            // Forward data to MRD Protocol Adapter for parsing
            // mrdAdapter.onDataReceived(data)
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
    
    fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic) {
        when (characteristic.uuid) {
            batteryCharUuid -> {
                val data = characteristic.value
                if (data.isNotEmpty()) {
                    _batteryLevel.value = data[0].toInt().coerceIn(0, 100)
                }
            }
        }
    }
}