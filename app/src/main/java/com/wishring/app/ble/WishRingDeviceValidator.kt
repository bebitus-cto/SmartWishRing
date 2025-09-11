package com.wishring.app.ble

import android.bluetooth.BluetoothDevice
import android.util.Log
import com.wishring.app.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WISH RING 기기 검증 클래스
 * 3단계 검증을 통해 연결된 기기가 실제 호환 가능한 WISH RING인지 확인
 */
@Singleton
class WishRingDeviceValidator @Inject constructor(
    private val mrdProtocolAdapter: MrdProtocolAdapter,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    
    companion object {
        private const val TAG = "WishRingValidator"
    }
    
    /**
     * WISH RING 기기 검증 결과
     */
    data class ValidationResult(
        val isValid: Boolean,
        val deviceName: String?,
        val hasCorrectUuid: Boolean,
        val sdkCommunicationSuccess: Boolean,
        val batteryLevel: Int?,
        val firmwareVersion: String?,
        val errorMessage: String? = null
    )
    
    /**
     * 3단계 검증을 수행하여 WISH RING 기기 여부 확인
     * 
     * @param device 검증할 블루투스 기기
     * @param serviceUuids 서비스 UUID 목록 (스캔 시 발견된)
     * @return 검증 결과
     */
    suspend fun validateWishRingDevice(
        device: BluetoothDevice,
        serviceUuids: List<String>?
    ): ValidationResult = withContext(ioDispatcher) {
        
        Log.d(TAG, "Starting validation for device: ${device.address}")
        
        try {
            // 1단계: 기기명 검증
            val deviceName = device.name
            val nameValid = validateDeviceName(deviceName)
            
            Log.d(TAG, "Device name validation: $nameValid (name: $deviceName)")
            
            // 2단계: Service UUID 검증
            val uuidValid = validateServiceUuid(serviceUuids)
            
            Log.d(TAG, "Service UUID validation: $uuidValid")
            
            // 3단계: SDK 통신 검증 (가장 중요)
            val sdkResult = validateSdkCommunication(device)
            
            Log.d(TAG, "SDK communication validation: ${sdkResult != null}")
            
            // 결과 종합
            val isValid = nameValid && uuidValid && (sdkResult != null)
            
            ValidationResult(
                isValid = isValid,
                deviceName = deviceName,
                hasCorrectUuid = uuidValid,
                sdkCommunicationSuccess = sdkResult != null,
                batteryLevel = sdkResult?.batteryLevel,
                firmwareVersion = sdkResult?.firmwareVersion,
                errorMessage = if (!isValid) generateErrorMessage(nameValid, uuidValid, sdkResult != null) else null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Validation failed with exception", e)
            
            ValidationResult(
                isValid = false,
                deviceName = device.name,
                hasCorrectUuid = false,
                sdkCommunicationSuccess = false,
                batteryLevel = null,
                firmwareVersion = null,
                errorMessage = "Validation exception: ${e.message}"
            )
        }
    }
    
    /**
     * 1단계: 기기명 검증
     * WISH RING 관련 접두사를 가지고 있는지 확인
     */
    private fun validateDeviceName(deviceName: String?): Boolean {
        if (deviceName.isNullOrBlank()) {
            Log.w(TAG, "Device name is null or blank")
            return false
        }
        
        return BleConstants.isWishRingDeviceName(deviceName)
    }
    
    /**
     * 2단계: Service UUID 검증
     * 스캔 시 발견된 서비스 UUID에 WISH RING 서비스가 포함되어 있는지 확인
     */
    private fun validateServiceUuid(serviceUuids: List<String>?): Boolean {
        if (serviceUuids.isNullOrEmpty()) {
            Log.w(TAG, "No service UUIDs provided")
            // UUID 정보가 없어도 기기명과 SDK 검증이 통과하면 허용
            return true
        }
        
        val hasWishRingService = serviceUuids.any { uuid ->
            uuid.equals(BleConstants.SERVICE_UUID, ignoreCase = true)
        }
        
        Log.d(TAG, "Service UUIDs: $serviceUuids")
        Log.d(TAG, "Has WISH RING service: $hasWishRingService")
        
        return hasWishRingService
    }
    
    /**
     * 3단계: SDK 통신 검증 (핵심)
     * 실제 MRD SDK를 사용하여 기기와 통신이 가능한지 확인
     */
    private suspend fun validateSdkCommunication(device: BluetoothDevice): SdkValidationResult? {
        
        Log.d(TAG, "Starting SDK communication validation")
        
        return withTimeoutOrNull(BleConstants.SDK_VALIDATION_TIMEOUT_MS) {
            try {
                // MRD SDK를 통한 시스템 정보 요청
                val systemInfo = mrdProtocolAdapter.getSystemInfo(SystemInfoType.BATTERY)
                
                if (systemInfo != null) {
                    Log.d(TAG, "SDK communication successful")
                    
                    // 추가 정보 수집
                    val batteryLevel = mrdProtocolAdapter.getBatteryLevel()
                    val firmwareVersion = mrdProtocolAdapter.getFirmwareVersion()
                    
                    SdkValidationResult(
                        success = true,
                        batteryLevel = batteryLevel,
                        firmwareVersion = firmwareVersion
                    )
                } else {
                    Log.w(TAG, "SDK communication returned null")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "SDK communication failed", e)
                null
            }
        }
    }
    
    /**
     * SDK 검증 결과 데이터 클래스
     */
    private data class SdkValidationResult(
        val success: Boolean,
        val batteryLevel: Int?,
        val firmwareVersion: String?
    )
    
    /**
     * 에러 메시지 생성
     */
    private fun generateErrorMessage(
        nameValid: Boolean, 
        uuidValid: Boolean, 
        sdkValid: Boolean
    ): String {
        val failures = mutableListOf<String>()
        
        if (!nameValid) failures.add("Invalid device name")
        if (!uuidValid) failures.add("Missing WISH RING service UUID")
        if (!sdkValid) failures.add("SDK communication failed")
        
        return "Device validation failed: ${failures.joinToString(", ")}"
    }
    
    /**
     * 빠른 검증 - 기기명만으로 1차 필터링
     * 스캔 시 사용하여 성능 최적화
     */
    fun quickValidation(deviceName: String?): Boolean {
        return validateDeviceName(deviceName)
    }
    
    /**
     * 연결된 기기의 추가 정보 수집
     * 검증 통과 후 상세 정보 획득용
     */
    suspend fun getDeviceDetailInfo(device: BluetoothDevice): DeviceDetailInfo? {
        return withContext(ioDispatcher) {
            try {
                val batteryLevel = mrdProtocolAdapter.getBatteryLevel()
                val firmwareVersion = mrdProtocolAdapter.getFirmwareVersion()
                val systemInfo = mrdProtocolAdapter.getSystemInfo(SystemInfoType.DEVICE_INFO)
                
                DeviceDetailInfo(
                    deviceName = device.name ?: "Unknown",
                    deviceAddress = device.address,
                    batteryLevel = batteryLevel,
                    firmwareVersion = firmwareVersion,
                    systemInfo = systemInfo,
                    bondState = device.bondState,
                    rssi = null // RSSI는 스캔 시점에서만 가져올 수 있음
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get device detail info", e)
                null
            }
        }
    }
    
    /**
     * 기기 상세 정보 데이터 클래스
     */
    data class DeviceDetailInfo(
        val deviceName: String,
        val deviceAddress: String,
        val batteryLevel: Int?,
        val firmwareVersion: String?,
        val systemInfo: String?,
        val bondState: Int,
        val rssi: Int?
    )
}

/**
 * 시스템 정보 타입 열거형
 */
enum class SystemInfoType {
    BATTERY,
    FIRMWARE_VERSION,
    DEVICE_INFO,
    SCREEN_INFO
}