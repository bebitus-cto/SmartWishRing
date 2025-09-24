package com.wishring.app.ble.model

import android.bluetooth.BluetoothGatt

/**
 * BLE GATT 연결 상태를 나타내는 sealed class
 * Repository에서 GATT 상태를 Flow로 전달하기 위해 사용
 */
sealed class GattState {
    /**
     * 연결이 끊어진 상태
     */
    object Disconnected : GattState()
    
    /**
     * GATT 연결된 상태
     * @property gatt 연결된 BluetoothGatt 객체
     */
    data class Connected(val gatt: BluetoothGatt) : GattState()
    
    /**
     * 서비스 발견 완료 상태
     * @property gatt 서비스가 발견된 BluetoothGatt 객체
     */
    data class ServicesDiscovered(val gatt: BluetoothGatt) : GattState()
    
    /**
     * 연결 오류 상태
     * @property message 오류 메시지
     */
    data class Error(val message: String) : GattState()
}