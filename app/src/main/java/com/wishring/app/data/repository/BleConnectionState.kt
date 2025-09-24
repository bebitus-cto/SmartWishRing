package com.wishring.app.data.repository

/**
 * BLE 연결 상태를 나타내는 enum
 */
enum class BleConnectionState {
    DISCONNECTED,    // 연결 안 됨
    SCANNING,        // 스캔 중
    CONNECTING,      // 연결 중
    CONNECTED,       // 연결됨
    DISCONNECTING,   // 연결 해제 중
    ERROR           // 오류 발생
}