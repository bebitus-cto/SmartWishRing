package com.wishring.app.ble.managers

import android.bluetooth.BluetoothDevice
import android.util.Log
// MRD SDK imports - mock implementation due to SDK issues
import com.wishring.app.domain.repository.BleConnectionState
import com.wishring.app.domain.repository.isConnected
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
 * Manages device counter and battery operations only
 * Mock implementation due to MRD SDK integration issues
 */
class BleDeviceManager(
    private val ioDispatcher: CoroutineDispatcher,
    private val getConnectionState: () -> BleConnectionState,
    private val getCurrentDevice: () -> BluetoothDevice?,
    private val getBatteryLevel: suspend () -> Int?
) {
    
    companion object {
        private const val TAG = "BleDeviceManager"
    }
    
    // MRD SDK Counter Integration - Mock implementation DISABLED
    val counterIncrements: Flow<Int> = callbackFlow {
        Log.d(TAG, "Counter callbacks disabled (mock removed)")
        
        // TODO: When MRD SDK is properly integrated, replace with actual implementation:
        // Manridy.getInstance().setMrdReadCallBack { type, data ->
        //     when (type) {
        //         MrdReadEnum.HEART -> trySend(1)
        //     }
        // }
        
        // Mock implementation REMOVED - was causing false button press detection
        // Users reported: "자꾸 WISH RING을 내가 위시링에서 카운트 증가를 하지도 않았늗네 증가가 감지됐따고 나옴"
        
        awaitClose { 
            Log.d(TAG, "Cleaning up counter callbacks")
        }
    }
    
    /**
     * Request battery level update from device
     * 기기로부터 배터리 레벨 업데이트 요청
     */
    suspend fun requestBatteryUpdate(): Result<Unit> {
        return try {
            if (!getConnectionState().isConnected()) {
                Log.w(TAG, "Cannot request battery - device not connected")
                Result.failure(IllegalStateException("Device not connected"))
            } else {
                // TODO: When MRD SDK is properly integrated:
                // Manridy.getMrdSend().getSystem(SystemEnum.battery, 1)
                
                Log.d(TAG, "Requesting battery update (mock)")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request battery update", e)
            Result.failure(e)
        }
    }
}