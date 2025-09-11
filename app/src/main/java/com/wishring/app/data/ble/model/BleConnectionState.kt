package com.wishring.app.data.ble.model

/**
 * BLE connection state enum
 * 
 * BLE 연결 상태를 나타내는 열거형
 */
enum class BleConnectionState {
    DISCONNECTED,    // 연결 해제 상태
    SCANNING,        // 디바이스 스캔 중
    CONNECTING,      // 연결 시도 중
    CONNECTED,       // 연결됨
    DISCONNECTING    // 연결 해제 중
}