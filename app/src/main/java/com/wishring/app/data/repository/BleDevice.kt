package com.wishring.app.data.repository

/**
 * BLE 장치 정보를 나타내는 데이터 클래스
 */
data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val isConnectable: Boolean,
    val isBonded: Boolean,
    val serviceUuids: List<String> = emptyList()
)